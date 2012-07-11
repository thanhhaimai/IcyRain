#!/usr/bin/python

import io
import struct

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
    buf.write(struct.pack('<I', duration))
    buf.write(struct.pack('B', 1)) # nr_motors
    buf.write(struct.pack('B', 3)) # motor_id
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
    print('GETB: ', u8)
    if not len(u8):
        return 0
    else:
        return struct.unpack('B', u8)[0]

def once_ready(f, tries=0):
    if not tries:
        print('Waiting to receive IDLE state...')

    s0 = 0
    while s0 != 0x2F:
        s0 = getbyte()
    s1 = getbyte()
    if s1 != 0x7F:
        once_ready(f, tries + 1)
    s2 = getbyte()
    if s2 != 0x06:
        print('Got a valid incoming message header: unsure what to do with it.')
        once_ready(f, tries + 1)
    
    print('Calling ', f)
    f()

def main():
    once_ready(test_echo)
    once_ready(test_print)
    once_ready(test_vibrate)

if __name__ == '__main__':
    conn = serial.Serial('/dev/ttyUSB0')
    main()
    conn.close()
