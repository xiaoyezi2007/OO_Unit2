import numpy as np

# ---参数区---
# 最大的乘客ID，最小的乘客ID为1
MAX_PASSENGER_ID = 2147483646
# 最大的电梯ID，最小的电梯ID为1
MAX_ELEVATOR_ID = 6
# 最小的楼层
MIN_FLOOR = 1
# 最大的楼层
MAX_FLOOR = 11
# 最晚的输入时间
MAX_TIME = 50.0
# 平均数据间隔时间
EXPECT_GAP_TIME = 0.8
# 最大数据间隔时间，必须小于最晚的输入时间的1/2
MAX_GAP_TIME = 10.0
assert 2 * MAX_GAP_TIME <= MAX_TIME
# 最大指令数量
MAX_INSTRUCTION = 70
# 两条指令之间有时间间隔的概率
INSTRUCTION_GAP_PROBABILITY = 0.3


class Generator(object):
    input_string_list = []
    NOW_TIME = 0.0
    NOW_INSTRUCTION = 0
    NOW_USED_PASSENGER_ID = []
    MAX_PASSENGER_ID = 1000
    MAX_TIME = 70.0
    MAX_INSTRUCTION = 103  # 100乘客+3更新
    EXPECT_GAP_TIME = 1.0
    MAX_GAP_TIME = 5.0
    INSTRUCTION_GAP_PROBABILITY = 0.3  # 时间间隔概率

    def get_input_string(self):
        return "\n".join(self.input_string_list)

    def generate(self):
        self.init()

        while (self.NOW_TIME <= MAX_TIME and (self.passenger_count < 100 or self.update_count < 3) and self.NOW_INSTRUCTION < MAX_INSTRUCTION):

            time_str = f"[{self.NOW_TIME:.1f}]"
            can_update = (self.update_count < 3) and (len(self.available_elevators()) >= 2 )
            p = np.random.uniform(0, 100)

            # 生成更新指令的条件判断
            if can_update and p < 15:  # 适当提高更新指令概率
                update_str = self.generate_update()
                if update_str:
                    self.input_string_list.append(f"{time_str}{update_str}")
                    self.update_count += 1
                    self.NOW_INSTRUCTION += 1
                    self.add_time()
            else:
                if self.passenger_count < 100:
                    passenger_str = self.generate_passenger()
                    self.input_string_list.append(f"{time_str}{passenger_str}")
                    self.passenger_count += 1
                    self.NOW_INSTRUCTION += 1
                    self.add_time()

            # 最后按实际时间戳排序（确保非严格递增）
            self.input_string_list.sort(key=lambda x: float(x[1:x.index(']')]))
        return self.input_string_list

    def init(self):
        self.input_string_list = []
        self.NOW_TIME = 1.0  # 起始时间
        self.NOW_INSTRUCTION = 0
        self.used_passenger_ids = set()
        self.used_elevator_ids = set()
        self.passenger_count = 0
        self.update_count = 0

    def generate_passenger(self):
        pid = self.generate_passenger_id()
        pri = np.random.randint(1, 100)
        from_floor, to_floor = self.generate_from_to_floor()
        elevator_id = np.random.randint(1, MAX_ELEVATOR_ID + 1)
        return f"{pid}-PRI-{pri}-FROM-{from_floor}-TO-{to_floor}"

    def generate_passenger_id(self):
        while True:
            pid = np.random.randint(1, MAX_PASSENGER_ID)
            if pid not in self.NOW_USED_PASSENGER_ID:
                self.NOW_USED_PASSENGER_ID.append(pid)
                return pid

    def generate_from_to_floor(self):
        floors = ["B4", "B3", "B2", "B1", "F1", "F2", "F3", "F4", "F5", "F6", "F7"]
        from_floor = np.random.choice(floors)
        to_floor = np.random.choice(floors)
        while to_floor == from_floor:
            to_floor = np.random.choice(floors)
        return from_floor, to_floor

    def add_time(self):
        if np.random.rand() <= INSTRUCTION_GAP_PROBABILITY:
            add_time = np.random.exponential(EXPECT_GAP_TIME)
            add_time = min(max(round(add_time, 1), 0.1), MAX_GAP_TIME)
            self.NOW_TIME = round(self.NOW_TIME + add_time, 1)

            # 强制不超过最大时间
            if self.NOW_TIME > MAX_TIME:
                self.NOW_TIME = MAX_TIME

    def available_elevators(self):
        return [id for id in range(1, 7) if id not in self.used_elevator_ids]

    def generate_update(self):
        available = self.available_elevators()
        if len(available) < 2:
            return None

        a, b = np.random.choice(available, size=2, replace=False)
        self.used_elevator_ids.update([a, b])

        target_floors = ["B2", "B1", "F1", "F2", "F3", "F4", "F5"]
        target = np.random.choice(target_floors)
        return f"UPDATE-{a}-{b}-{target}"



