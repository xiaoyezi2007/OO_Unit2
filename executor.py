import os
import sys
import traceback

from subprocess import Popen, PIPE, TimeoutExpired
from threading import Thread

from generater import Generator
from judger import Judger

# 最大等待时间，单位为秒
MAX_TIMEOUT = 120


class Executor(Thread):
    all_epoch = 0
    jar_filename = ""
    thread_dir_name = ""
    data_dir_name = ""
    result_dir_name = ""

    Gen = Generator()
    Jud = Judger()

    def __init__(self, thread_name, all_epoch, jar_filename, work_dir_name):
        super().__init__(name=thread_name)
        self.all_epoch = all_epoch
        self.jar_filename = jar_filename
        # 建立线程文件夹、输入文件夹和结果文件夹
        self.thread_dir_name = work_dir_name + "/thread-" + thread_name
        self.data_dir_name = self.thread_dir_name + "/data"
        self.result_dir_name = self.thread_dir_name + "/result"
        self.judge_dir_name = self.thread_dir_name + "/judge"
        os.mkdir(self.thread_dir_name)
        os.mkdir(self.data_dir_name)
        os.mkdir(self.result_dir_name)
        os.mkdir(self.judge_dir_name)

    def run(self):
        for epoch in range(self.all_epoch):
            print("Epoch " + str(epoch + 1) + "/" + str(self.all_epoch) + " of Thread-" + self.name)

            epoch_filename = "/epoch-" + str(epoch + 1) + ".txt"
            # 生成数据并写入文件
            self.Gen.generate()
            data_file = open(self.data_dir_name + epoch_filename, mode="w")
            data_file.write(self.Gen.get_input_string())
            data_file.close()

            input_command = ["java", "-jar", "./Input.jar", self.data_dir_name + epoch_filename]
            test_command = ["java", "-jar", "./JAR/judge/" + self.jar_filename + ".jar"]

            input_process = Popen(input_command, stdout=PIPE)
            test_process = Popen(test_command, stdin=input_process.stdout,
                                 stdout=open(self.result_dir_name + epoch_filename, mode="w"))

            try:
                test_process.wait(timeout=MAX_TIMEOUT)
                self.Jud.judge(data_filename=self.data_dir_name + epoch_filename,
                               result_filename=self.result_dir_name + epoch_filename,
                               judge_filename=self.judge_dir_name + epoch_filename)
            except TimeoutExpired:
                self.write_judge_file(epoch_filename,
                                      "TimeoutExpired: Java didn't terminate in " + str(MAX_TIMEOUT) + " seconds")
            except Exception:
                exc_info = sys.exc_info()
                self.write_judge_file(epoch_filename,
                                      "Exception: " + str(exc_info[0]) + "\n" + str(exc_info[1]) + "\n"
                                      + str(traceback.format_tb(exc_info[2])))

    def write_judge_file(self, epoch_filename, result):
        judge_file = open(self.judge_dir_name + epoch_filename, mode="w")
        judge_file.write(result)
        judge_file.close()
