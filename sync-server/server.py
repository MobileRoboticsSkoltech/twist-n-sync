import socketserver
import socket
from TimeSync import TimeSync2
import pandas as pd
import numpy as np

from struct import pack


BUFFER_SIZE = 2048


def main():
    host = '192.168.1.61'
    port = 9428

    s = socket.socket()
    s.bind((host, port))
    print("Server Started")
    s.listen(2)

    while (True):
        print("Session started, waiting for two gyro files")
        for i in range(2):                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
            sc, address = s.accept()

            print(f"Connected to {address}")
            f = open('gyro_file_' + str(i) + ".csv",'wb')
            l = sc.recv(BUFFER_SIZE)
            while (l):
                f.write(l)
                l = sc.recv(BUFFER_SIZE)
            f.close()
            sc.close()
            print(f"Received gyro file {i}")

        
        # Perform sync, gyro 1 is client file, gyro 2 - leader
        gyro1 = pd.read_csv("gyro_file_0.csv", names=['x', 'y', 'z', 't'])
        gyro2 = pd.read_csv("gyro_file_1.csv", names=['x', 'y', 'z', 't'])                          

        gyro1_values = gyro1.drop("t", axis=1)
        gyro2_values = gyro2.drop("t", axis=1)

        t1 = gyro1.t / 1e9
        t2 = gyro2.t / 1e9


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
    s.close()

if __name__ == '__main__':
    main()

