#define PCAP_FILE_HDR_MAGIC 0xa1b2c3d4
#define PKT_HDR_LEN sizeof(struct pcap_pkthdr)
#define PKT_MAX_LEN 65535

struct pcap_file_header {
        u_int magic;
        u_short version_major;
        u_short version_minor;
        int thiszone;     /* gmt to local correction */
        u_int sigfigs;    /* accuracy of timestamps */
        u_int snaplen;    /* max length saved portion of each pkt */
        u_int linktype;   /* data link type (LINKTYPE_*) */
};

struct pcap_pkthdr {
        struct timeval ts;      /* time stamp */
        u_int caplen;     /* length of portion present */
        u_int len;        /* length this packet (off wire) */
};
