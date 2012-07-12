#!/usr/bin/python

import io
import struct

import time

import serial

def test_echo(op=0):
    send_message(op, io.BytesIO())

def test_print(op=1):
    buf = io.BytesIO()
    buf.write(b'Heroo?')
    send_message(op, buf)

def test_vibrate(op=3):
    buf = io.BytesIO()
    duration = 5000
    buf.write(struct.pack('<H', duration))
    buf.write(struct.pack('B', 1)) # nr_motors
    buf.write(struct.pack('B', 6)) # motor_id
    buf.write(struct.pack('B', 255)) # magnitude
    send_message(op, buf)

def test_set_state(op=4):
    pass

def test_query(op=5):
    pass

def send_message(op, buf):
    msg = buf.getvalue()
    print('Sending message: ', msg)
    conn.write(b'\x2F')
    conn.write(b'\x75')
    conn.write(struct.pack('B', op))
    conn.write(struct.pack('B', len(msg)))
    cksum = len(msg)
    for u8 in msg:
        cksum ^= int(u8)
        cksum %= 48
    conn.write(msg)
    conn.write(struct.pack('B', cksum))
    buf.close()

def getbyte():
    u8 = conn.read(1)
    print('GETB:', u8)
    if not len(u8):
        return 0
    else:
        return struct.unpack('B', u8)[0]

def once_ready(f):
    print('Waiting to receive IDLE state...')
    state = 0
    opcode = None
    while True:
        u8 = getbyte()

        if state == 0:
            if u8 == 0x2F:
                state = 1
            else:
                state = 0
            continue

        if state == 1:
            if u8 == 0x75:
                state = 2
            else:
                state = 0
            continue

        if state == 2:
            opcode = u8
            break

    if opcode == 6:
        print('Got IDLE, calling ', f)
        f()

def main():
    once_ready(test_echo)
    once_ready(test_print)
    once_ready(test_vibrate)

def dump():
    while True:
        if conn.inWaiting():
            print(conn.read(conn.inWaiting()))
        time.sleep(1)

def drain():
    print('Draining input stream...')
    for i in range(3):
        time.sleep(1)
        conn.read(conn.inWaiting())

if __name__ == '__main__':
    conn = serial.Serial('/dev/ttyACM0')
    drain()
    main()
    dump()
    conn.close()
