import os
import time

from executor import Executor

print('The jar file should be put in ./JAR/judge/ and named as (prefix)(number).jar:')

# jar文件
jar_prefix = input("Please input the jar file prefix: ")
jar_number = input("Please input the jar file number which should be tested: ")
jar_filename = jar_prefix + str(jar_number)

# 迭代轮数
all_thread = input("Please input the thread(s): ")
all_thread = int(all_thread)

# 每轮迭代线程数
epoch_each_thread = input("Please input the epoch of each thread: ")
epoch_each_thread = int(epoch_each_thread)

# 初次运行创建jar文件夹
jar_dir_name = "./runtime/" + jar_filename
try:
    os.mkdir(jar_dir_name)
except FileExistsError:
    print("The jar directory is already created!")

# 创建该次运行的文件夹
work_dir_name = jar_dir_name + "/" + time.strftime("%m%d_%H%M%S", time.localtime())
os.mkdir(work_dir_name)

for thread in range(all_thread):
    print("Thread " + str(thread + 1) + " of " + str(all_thread))
    Exe = Executor(thread_name=str(thread + 1), all_epoch=epoch_each_thread,
                   jar_filename=jar_filename, work_dir_name=work_dir_name)
    Exe.start()
