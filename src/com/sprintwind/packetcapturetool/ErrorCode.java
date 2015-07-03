package com.sprintwind.packetcapturetool;

public class ErrorCode {
public static int OK = 0x0;
public static int INVALID_LEN = 0x01;
public static int UNKOWN_ETH_PROTO = 0x02;
public static int INVALID_BIT_RANGE = 0x03;
public static int FILE_NOT_EXIST = 0x04;
public static int INVALID_PCAP_HEADER = 0x05;
public static int INVALID_PCAP_PACKET = 0x06;
public static int IO_EXCEPTION = 0x07;
public static int FILE_NOT_FOUND_EXCEPTION = 0x08;

}