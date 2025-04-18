import math
import re
import sys
import traceback

# 电梯参数
ELEVATOR_VOLUME = 6
MAX_ELEVATOR_ID = 6
_CLOSE = 0
_OPEN = 1
_UPDATE = 2


class Judger:
    # 楼层映射字典（B4→-4, F1→0, F7→6）
    floor_map = {
        "B4": -4, "B3": -3, "B2": -2, "B1": -1,
        "F1": 0, "F2": 1, "F3": 2, "F4": 3,
        "F5": 4, "F6": 5, "F7": 6
    }
    reverse_floor_map = {v: k for k, v in floor_map.items()}

    def __init__(self):
        self.data_filename = ""
        self.result_filename = ""
        self.judge_filename = ""
        self.data_file = None
        self.result_file = None
        self.judge_file = None
        self.passenger_dict = {}  # 存储乘客信息
        self.elevator_list = []  # 存储电梯状态
        self.elevator_timers = {}  # 记录电梯开关门时间

    def judge(self, data_filename, result_filename, judge_filename):
        self.init(data_filename, result_filename, judge_filename)
        data_lines = self.data_file.readlines()
        result_lines = self.result_file.readlines()

        if not data_lines:
            self.judge_file.write("EmptyFile: Input data file is empty!")
            return
        if not result_lines:
            self.judge_file.write("EmptyFile: Output result file is empty!")
            return

        try:
            if self.parse_data(data_lines):
                if self.judge_result(result_lines):
                    self.judge_file.write("AC")
                else:
                    self.judge_file.write("WA")
        except Exception as e:
            exc_info = sys.exc_info()
            error_msg = f"Exception: {exc_info[0].__name__}: {exc_info[1]}\n"
            error_msg += "".join(traceback.format_tb(exc_info[2]))
            self.judge_file.write(error_msg)
        finally:
            self.cleanup()

    # ----------------- 初始化与清理 -----------------
    def init(self, data_filename, result_filename, judge_filename):
        self.data_filename = data_filename
        self.result_filename = result_filename
        self.judge_filename = judge_filename
        self.data_file = open(data_filename, "r")
        self.result_file = open(result_filename, "r")
        self.judge_file = open(judge_filename, "w")
        self.passenger_dict = {}
        # 初始化电梯状态：初始楼层为F1（数值0），状态为关门
        self.elevator_list = [
            {"floor": 0, "passenger": 0, "status": _CLOSE, "open_time": None, "MaxFloor": 6, "MinFloor": -4, "Received": set(), "TargetFloor": -100}
            for _ in range(MAX_ELEVATOR_ID)
        ]

    def cleanup(self):
        self.data_file.close()
        self.result_file.close()
        self.judge_file.close()

    # ----------------- 输入文件解析 -----------------
    def parse_data(self, data_lines):
        for line_num, line in enumerate(data_lines, 1):
            line = line.strip()
            pattern = r'\[\s*([0-9]+\.[0-9]+)\s*\]([0-9]+)-PRI-([0-9]+)-FROM-(B4|B3|B2|B1|F1|F2|F3|F4|F5|F6|F7)-TO-(B4|B3|B2|B1|F1|F2|F3|F4|F5|F6|F7)'
            match = re.fullmatch(pattern, line)
            if not match:
                continue
            _, pid, pri, from_floor, to_floor = match.groups()

            self.passenger_dict[int(pid)] = {
                "from": self.floor_map[from_floor],
                "to": self.floor_map[to_floor],
                "eid": -1,
                "entered": False,
                "exited": False
            }
        return True

    # ----------------- 输出结果验证 -----------------
    def judge_result(self, result_lines):
        for line_num, line in enumerate(result_lines, 1):
            line = line.strip()
            if not line:
                continue

            # 根据事件类型分发处理函数
            if "ARRIVE" in line:
                handler = self.judge_arrive
            elif "OPEN" in line:
                handler = self.judge_open
            elif "CLOSE" in line:
                handler = self.judge_close
            elif "ACCEPT" in line:
                handler = self.judge_updateAccept
            elif "UPDATE" in line:
                handler = self.judge_update
            elif "IN" in line:
                handler = self.judge_in
            elif "OUT" in line:
                handler = self.judge_out
            elif "RECEIVE" in line:
                handler = self.judge_receive
            else:
                self.report_error("FormatError", f"未知事件类型（行{line_num}）：{line}")
                return False

            # 统一调用处理函数，仅传递 line 和 line_num
            if not handler(line, line_num):
                return False

        return self.check_all_passengers()


    # ----------------- 具体事件验证 -----------------
    def judge_arrive(self, line, line_num):
        pattern = r'\[\s*([0-9]+\.[0-9]+)\s*\]\s*ARRIVE-(B4|B3|B2|B1|F1|F2|F3|F4|F5|F6|F7)-([1-6])'
        match = re.match(pattern, line)
        if not match:
            self.report_error("FormatError", f"ARRIVE format error in line {line_num}: {line}")
            return False

        timestamp, floor_str, eid = match.groups()
        eid = int(eid) - 1  # 转为0-based索引
        floor = self.floor_map.get(floor_str)
        if floor is None:
            self.report_error("WA", f"Invalid floor {floor_str} in line {line_num}")
            return False
        elevator = self.elevator_list[eid]

        if (len(elevator["Received"]) == 0 and ((elevator["floor"] != elevator["MaxFloor"] and elevator["MaxFloor"] != 6) or (elevator["floor"] != elevator["MinFloor"] and elevator["MinFloor"] != -4))):
            self.report_error("WA", f"Elevator {eid + 1} cannot move because there no receive in line {line_num}")
            return False
        if (floor < elevator["MinFloor"]) or (floor > elevator["MaxFloor"]):
            self.report_error("WA", f"Elevator {eid + 1} cannot arrive Invalid floor {floor_str} in line {line_num}")
            return False
        # 检查电梯是否在移动前关门
        if elevator["status"] == _OPEN:
            self.report_error("WA", f"Elevator {eid + 1} moved while open in line {line_num}")
            return False
        # 检查移动是否合法（每次只能移动1层）
        if abs(floor - elevator["floor"]) != 1:
            self.report_error("WA", f"Elevator {eid + 1} jumped floors in line {line_num}")
            return False

        elevator["floor"] = floor
        return True

    def judge_open(self, line, line_num):
#        pattern = r'\[\s*([0-9]+\.[0-9]+)\]\s*OPEN-([B4B3B2B1F1F2F3F4F5F6F7]+)-([1-6])'
        pattern = r'\[\s*([0-9]+\.[0-9]+)\s*\]\s*OPEN-(B4|B3|B2|B1|F1|F2|F3|F4|F5|F6|F7)-([1-6])'
        match = re.fullmatch(pattern, line)
        if not match:
            self.report_error("FormatError", f"OPEN format error in line {line_num}: {line}")
            return False

        timestamp, floor_str, eid = match.groups()
        eid = int(eid) - 1
        floor = self.floor_map[floor_str]
        elevator = self.elevator_list[eid]

        # 检查是否重复开门
        if elevator["status"] == _OPEN:
            self.report_error("WA", f"Elevator {eid + 1} already open in line {line_num}")
            return False
        # 检查楼层是否正确
        if elevator["floor"] != floor:
            self.report_error("WA", f"Elevator {eid + 1} opened at wrong floor in line {line_num}")
            return False

        elevator["status"] = _OPEN
        elevator["open_time"] = float(timestamp)
        return True

    def judge_close(self, line, line_num):
#        pattern = r'\[\s*([0-9]+\.[0-9]+)\]\s*CLOSE-([B4B3B2B1F1F2F3F4F5F6F7]+)-([1-6])'
        pattern = r'\[\s*([0-9]+\.[0-9]+)\s*\]\s*CLOSE-(B4|B3|B2|B1|F1|F2|F3|F4|F5|F6|F7)-([1-6])'
        match = re.fullmatch(pattern, line)
        if not match:
            self.report_error("FormatError", f"CLOSE format error in line {line_num}: {line}")
            return False

        timestamp, floor_str, eid = match.groups()
        eid = int(eid) - 1
        floor = self.floor_map[floor_str]
        elevator = self.elevator_list[eid]
        close_time = float(timestamp)

        # 检查开关门时间间隔
        if elevator["open_time"] is None or (close_time - elevator["open_time"]-0.4) < -1e-7:
            self.report_error("WA", f"Door closed too quickly (<0.4s) in line {line_num} {close_time} {elevator["open_time"]} {(close_time - elevator["open_time"])}")
            return False
        # 检查状态和楼层
        if elevator["status"] != _OPEN:
            self.report_error("WA", f"Elevator {eid + 1} not open when closing in line {line_num}")
            return False
        if elevator["floor"] != floor:
            self.report_error("WA", f"Elevator {eid + 1} closed at wrong floor in line {line_num}")
            return False

        elevator["status"] = _CLOSE
        elevator["open_time"] = None
        return True

    def judge_in(self, line, line_num):
        pattern = r'\[\s*([0-9]+\.[0-9]+)\s*\]\s*IN-(\d+)-(B4|B3|B2|B1|F1|F2|F3|F4|F5|F6|F7)-([1-6])'
        match = re.fullmatch(pattern, line)

        if not match:
            self.report_error("FormatError", f"IN格式错误（行{line_num}）：{line}")
            return False
            # 提取字段
        _, pid_str, floor_str, eid = match.groups()
        pid = int(pid_str)
        eid = int(eid) - 1
        floor = self.floor_map[floor_str]
        elevator = self.elevator_list[eid]

        # 检查乘客是否存在
        if pid not in elevator["Received"]:
            self.report_error("WA", f"Passenger {pid} not received by elevator {eid} but IN in line {line_num}")
            return False
        passenger = self.passenger_dict[pid]
        # 检查电梯状态和楼层
        if elevator["status"] != _OPEN:
            self.report_error("WA", f"Passenger entered closed elevator in line {line_num}")
            return False
        if passenger["from"] != elevator["floor"]:
            self.report_error("WA", f"Passenger entered at wrong floor in line {line_num}")
            return False
        # 检查是否重复进入
        if passenger["entered"]:
            self.report_error("WA", f"Passenger {pid} entered twice in line {line_num}")
            return False

        passenger["entered"] = True
        elevator["passenger"] += 1
        if elevator["passenger"] > ELEVATOR_VOLUME:
            self.report_error("WA", f"Elevator {eid + 1} overloaded in line {line_num}")
            return False
        return True

    def judge_out(self, line, line_num):
        pattern = r'\[\s*([0-9]+\.[0-9]+)\s*\]\s*OUT-(F|S)-(\d+)-(B4|B3|B2|B1|F1|F2|F3|F4|F5|F6|F7)-([1-6])'
        match = re.fullmatch(pattern, line)
        if not match:
            self.report_error("FormatError", f"OUT format error in line {line_num}: {line}")
            return False

        _, type, pid_str, floor_str, eid = match.groups()
        pid = int(pid_str)
        eid = int(eid) - 1
        floor = self.floor_map[floor_str]
        elevator = self.elevator_list[eid]

        if pid not in self.passenger_dict:
            self.report_error("WA", f"Passenger {pid} not exists in line {line_num}")
            return False
        passenger = self.passenger_dict[pid]
        # 检查是否已进入电梯
        if not passenger["entered"]:
            self.report_error("WA", f"Passenger {pid} exited without entering in line {line_num}")
            return False
        # 检查目标楼层
        if type == "S" and passenger["to"] != elevator["floor"]:
            self.report_error("WA", f"Passenger exited at wrong floor in line {line_num}")
            return False
        if (type == "S"):
            passenger["exited"] = True

        passenger["entered"] = False
        passenger["eid"] = -1
        passenger["from"] = elevator["floor"]
        elevator["Received"].remove(pid)
        elevator["passenger"] -= 1
        return True

    def judge_receive(self, line, line_num):
        pattern = r'\[\s*([0-9]+\.[0-9]+)\s*\]\s*RECEIVE-(\d+)-([1-6])'
        match = re.fullmatch(pattern, line)
        if not match:
            self.report_error("FormatError", f"OUT format error in line {line_num}: {line}")
            return False

        _, pid_str, eid = match.groups()
        pid = int(pid_str)
        eid = int(eid) - 1
        elevator = self.elevator_list[eid]
        if pid not in self.passenger_dict:
            self.report_error("WA", f"Passenger {pid} not exists in line {line_num}")
            return False
        passenger = self.passenger_dict[pid]
        if passenger["eid"] != -1:
            self.report_error("WA", f"Passenger {pid} have received yet but receive again in line {line_num}")
            return False
        if elevator["status"] == _UPDATE:
            self.report_error("WA", f"Elevator {eid} receive when update in line {line_num}")
            return False

        passenger["eid"] = eid
        elevator["Received"].add(pid)
        return True

    def judge_update(self, line, line_num):
        pattern = r'\[\s*([0-9]+\.[0-9]+)\s*\]\s*UPDATE-(BEGIN|END)-([1-6])-([1-6])'
        match = re.fullmatch(pattern, line)
        if not match:
            self.report_error("FormatError", f"OUT format error in line {line_num}: {line}")
            return False

        _, type, eid_A, eid_B = match.groups()
        eid_B = int(eid_B) - 1
        eid_A = int(eid_A) - 1
        elevator_A = self.elevator_list[eid_A]
        elevator_B = self.elevator_list[eid_B]
        if elevator_A["status"] != _CLOSE or elevator_B["status"] != _CLOSE:
            self.report_error("WA", f"Elevator do not close the door when update in line {line_num}")
            return False
        if elevator_A["TargetFloor"] == -100 or elevator_B["TargetFloor"] == -100:
            self.report_error("WA", f"no update receive in {line_num}")
            return False
        if type == "BEGIN":
            for every in elevator_A["Received"]:
                passenger = self.passenger_dict[every]
                passenger["eid"] = -1
            elevator_A["Received"].clear()
            for every in elevator_B["Received"]:
                passenger = self.passenger_dict[every]
                passenger["eid"] = -1
            elevator_B["Received"].clear()
        else:
            elevator_A["MinFloor"] = elevator_A["TargetFloor"]
            elevator_A["floor"] = elevator_A["TargetFloor"] + 1
            elevator_B["MaxFloor"] = elevator_B["TargetFloor"]
            elevator_B["floor"] = elevator_B["TargetFloor"] - 1
        return True

    def judge_updateAccept(self, line, line_num):
        pattern = r'\[\s*([0-9]+\.[0-9]+)\s*\]\s*UPDATE-(ACCEPT)-([1-6])-([1-6])-(B2|B1|F1|F2|F3|F4|F5)'
        match = re.fullmatch(pattern, line)
        if not match:
            self.report_error("FormatError", f"OUT format error in line {line_num}: {line}")
            return False

        _, _, eid_A, eid_B, target_str = match.groups()
        eid_B = int(eid_B) - 1
        eid_A = int(eid_A) - 1
        elevator_A = self.elevator_list[eid_A]
        elevator_B = self.elevator_list[eid_B]
        target = self.floor_map[target_str]
        elevator_A["TargetFloor"] = target
        elevator_B["TargetFloor"] = target
        return True

    # ----------------- 最终检查 -----------------
    def check_all_passengers(self):
        for pid, info in self.passenger_dict.items():
            if not (info["exited"]):
                self.report_error("WA", f"Passenger {pid} not delivered")
                return False
        return True

    # ----------------- 错误报告工具 -----------------
    def report_error(self, error_type, message):
        self.judge_file.write(f"{error_type}: {message}\n")