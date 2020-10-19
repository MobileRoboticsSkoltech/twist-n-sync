import socketserver
import socket
from TimeSync import TimeSync2
import pandas as pd
import numpy as np

import os

from struct import pack

BUFFER_SIZE = 2048
SENSOR_FILES_SUBDIRECTORY = "sensors"


def main():
    host = get_ip()
    port = 9428

    s = socket.socket()
    s.bind((host, port))
    print(f'Server started, hostname: {host}. Put this value in PC_SERVER_IP constant of Constants.java file.')
    s.listen(2)
    session_id = 0;

    try:
        while (True):
            print(f"New session {session_id} started, waiting for two gyro files")
            gyro = []
            for i in range(2):                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
                sc, address = s.accept()

                print(f"Connected to {address}")
                if not os.path.exists(SENSOR_FILES_SUBDIRECTORY):
                    os.makedirs(SENSOR_FILES_SUBDIRECTORY)
                path = os.path.join(SENSOR_FILES_SUBDIRECTORY, f'gyro_{i}-session_{session_id}.csv')
                f = open(path,'wb')
                l = sc.recv(BUFFER_SIZE)
                while (l):
                    f.write(l)
                    l = sc.recv(BUFFER_SIZE)
                f.close()
                sc.close()
                
                print(f"Received gyro file {i}")
                gyro.append(pd.read_csv(path, names=['x', 'y', 'z', 't']))

            # gyro1 - leader, gyro2 - client 
            gyro1_values = gyro[0].drop("t", axis=1)
            gyro2_values = gyro[1].drop("t", axis=1)

            t1 = gyro[0].t / 1e9
            t2 = gyro[1].t / 1e9

            # Need equal sizes for resampling
            size = min(gyro1_values.shape[0], gyro2_values.shape[0])
            gyro1_abs = np.linalg.norm(gyro1_values, axis=1)[:size]
            gyro2_abs = np.linalg.norm(gyro2_values, axis=1)[:size]
            t1 = t1[:size]
            t2 = t2[:size]
            t1_np = t1.to_numpy()
            t2_np = t2.to_numpy()

            time_sync2 = TimeSync2(gyro1_abs, gyro2_abs, t1_np, t2_np, True)
            time_sync2.resample(accuracy=1)
            time_sync2.obtain_delay()
            comp_delay2 = time_sync2.time_delay
            
            # The time delta (leader - client)
            resulting_offset_s = (np.mean(t2 - t1) + comp_delay2)
            resulting_offset_ns = resulting_offset_s * 1e9
            
            # Return offset
            print(f"Summary offset from leader to client: {resulting_offset_s} seconds")
            print("Returning offset..")
            sc, address = s.accept()
            print(f"Connected to {address}")
            # Packing Double offset to bytes
            sc.send(
                pack(">d", resulting_offset_ns)
            )
            sc.close()
            session_id += 1
    except KeyboardInterrupt:
        print("Interrupted, closing connection..")
        s.close()
    finally:
        s.close()


def get_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 1))
        IP = s.getsockname()[0]
    except:
        IP = '127.0.0.1'
    finally:
        s.close()
    return IP

if __name__ == '__main__':
    main()

