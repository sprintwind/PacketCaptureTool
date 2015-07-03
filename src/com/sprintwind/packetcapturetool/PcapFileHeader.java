package com.sprintwind.packetcapturetool;

public class PcapFileHeader implements ByteInitializer{
static final int PCAP_FILE_HDR_LEN = 24;
static final int PCAP_FILE_MAGIC = 0xa1b2c3d4;

private int magic;
private short version_major;
private short version_minor;
private int thiszone;
/* gmt to local correction */
private int sigfings;
/* accuracy of timestamps */
private int snaplen;
/* max length saved portion of each pkt */
private int linktype;
/* data link type (LINKTYPE_*) */
public int getMagic() {
return magic;
}
public void setMagic(int magic) {
this.magic = magic;
}
public short getVersion_major() {
return version_major;
}
public void setVersion_major(short version_major) {
this.version_major = version_major;
}
public short getVersion_minor() {
return version_minor;
}
public void setVersion_minor(short version_minor) {
this.version_minor = version_minor;
}
public int getThiszone() {
return thiszone;
}
public void setThiszone(int thiszone) {
this.thiszone = thiszone;
}
public int getSigfings() {
return sigfings;
}
public void setSigfings(int sigfings) {
this.sigfings = sigfings;
}
public int getSnaplen() {
return snaplen;
}
public void setSnaplen(int snaplen) {
this.snaplen = snaplen;
}
public int getLinktype() {
return linktype;
}
public void setLinktype(int linktype) {
this.linktype = linktype;
}

public int initWithByteArray(byte[] array, int start) {
if(array.length < PCAP_FILE_HDR_LEN) {
return ErrorCode.INVALID_LEN;
}

int startTmp = start;

this.magic = ByteTranslater.hostIntFromByte(array, startTmp);
startTmp += ByteTranslater.BYTE_LEN_OF_INT;

this.version_major = ByteTranslater.hostShortFromByte(array, startTmp);
startTmp += ByteTranslater.BYTE_LEN_OF_SHORT;

this.version_minor = ByteTranslater.hostShortFromByte(array, startTmp);
startTmp += ByteTranslater.BYTE_LEN_OF_SHORT;

this.thiszone = ByteTranslater.hostIntFromByte(array, startTmp);
startTmp += ByteTranslater.BYTE_LEN_OF_INT;

this.sigfings = ByteTranslater.hostIntFromByte(array, startTmp);
startTmp += ByteTranslater.BYTE_LEN_OF_INT;

this.snaplen = ByteTranslater.hostIntFromByte(array, startTmp);
startTmp += ByteTranslater.BYTE_LEN_OF_INT;

this.linktype = ByteTranslater.hostIntFromByte(array, startTmp);

return ErrorCode.OK;
}

public void print() {
System.out.println("pcap file header info:");
System.out.println("------------------------------------");
System.out.println("magic:"+String.format("%x", this.magic));
System.out.println("major version:"+this.version_major);
System.out.println("minor version:"+this.version_minor);
System.out.println("snap length:"+this.snaplen);
System.out.println("link type:"+this.linktype);
System.out.println("------------------------------------");
}
}
