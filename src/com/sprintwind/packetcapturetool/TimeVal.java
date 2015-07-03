package com.sprintwind.packetcapturetool;

public class TimeVal implements ByteInitializer{
static final int TIME_VAL_LEN = 8;

int sec;
int uSec;

public int getSec() {
return sec;
}
public void setSec(int sec) {
this.sec = sec;
}
public int getuSec() {
return uSec;
}
public void setuSec(int uSec) {
this.uSec = uSec;
}

/**
* @param array array of byte
* @param start start index of the byte array
* @return ErrorCode.OK on success, others when failed.
*/
public int initWithByteArray(byte[] array, int start) {
if(array.length - start < TIME_VAL_LEN) {
return ErrorCode.INVALID_LEN;
}

sec = ByteTranslater.hostIntFromByte(array, 0);
uSec = ByteTranslater.hostIntFromByte(array, 0 + ByteTranslater.BYTE_LEN_OF_INT);

return ErrorCode.OK;
}

}