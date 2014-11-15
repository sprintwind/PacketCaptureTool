#include <jni.h>
#include <net/if.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <time.h>
#include <signal.h>
//#include <pcap.h>
#include <arpa/inet.h>
#include <netinet/if_ether.h>
#include <linux/filter.h>
#include <linux/if_packet.h>
#include <pthread.h>
#include <android/log.h>
#define KEY_ESC 27
#define MAX_INTERFACE_NUM 16
#define MAX_INTEGER_LEN 10

#define PCAP_VERSION_MAJOR 2
#define PCAP_VERSION_MINOR 4

#define FILE_NAME_LEN 256
#define CAP_FILE_DIR "packet_capture"
#define STATS_FILE ".cap_stats"
#define PCAP_FILE_SUFFIX "pcap"
#define PCAP_FILE_HDR_MAGIC 0xa1b2c3d4
#define PKT_HDR_LEN sizeof(struct pcap_pkthdr)
#define PKT_MAX_LEN 65535
#define BUFF_BKT_CNT_MAX 100
#define BUFF_SIZE ((BUFF_BKT_CNT_MAX)*(PKT_HDR_LEN+PKT_MAX_LEN))

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

int capture = 1;
char file_name[FILE_NAME_LEN] = {0};
char stats_file_dir[FILE_NAME_LEN-sizeof(STATS_FILE)] = {0};

struct pcap_pkthdr* ppkthdr;
char* buffer;
char* ppkt_start;
FILE* file;

time_t time_now;
int capture_count = 0;
int total_capture_count = 0;
int need_write_len;
int write_len;
int recv_len;
int sock_fd;

pthread_t thread_id;

#ifdef __cplusplus
extern "C" {
#endif

void init_file_header(struct pcap_file_header* pfile_hdr, int cap_len)
{
	pfile_hdr->magic = PCAP_FILE_HDR_MAGIC;
	pfile_hdr->version_major = PCAP_VERSION_MAJOR;
	pfile_hdr->version_minor = PCAP_VERSION_MINOR;
	pfile_hdr->thiszone = 0;
	pfile_hdr->sigfigs = 0;
	pfile_hdr->snaplen = cap_len;
	pfile_hdr->linktype = 1; /* ethernet */
}

/*
 * 抓包线程
 */
void* capture_thread(void* arg)
{
	while(capture)
	{
		usleep(10000);
		recv_len = recv(sock_fd, ppkt_start, PKT_MAX_LEN, MSG_DONTWAIT);
		if(recv_len < 0)
		{
			if(errno != EAGAIN)
			{
				__android_log_print(ANDROID_LOG_INFO, "sprintwind", "recv failed, %s", strerror(errno));
				return NULL;
			}

			continue;
		}

		gettimeofday(&ppkthdr->ts, NULL);
		ppkthdr->caplen = recv_len;
		ppkthdr->len = recv_len;

		//__android_log_print(ANDROID_LOG_INFO, "sprintwind", "recv a packet, packet len:%d\n", recv_len);

		capture_count++;
		total_capture_count++;
		need_write_len += (recv_len+PKT_HDR_LEN);
		//printf("captured %d\n", total_capture_count);
		//MOVE_UP(2);

		/* 达到缓存报文数量则开始写文件 */
		if(capture_count >= BUFF_BKT_CNT_MAX)
		{
			if(fwrite(buffer, sizeof(char), need_write_len, file) < need_write_len)
			{
				__android_log_print(ANDROID_LOG_INFO, "sprintwind", "fwrite failed");
				return NULL;
			}

			__android_log_print(ANDROID_LOG_INFO, "sprintwind", "write bufferred data %d bytes\n", need_write_len);

			/* 写完文件后重置相关变量 */
			ppkthdr = (struct pcap_pkthdr*)buffer;
			ppkt_start = (char*)ppkthdr + PKT_HDR_LEN;
			capture_count = 0;
			need_write_len = 0;

			continue;
		}

		/* 移动报文相关指针到当前报文后面 */
		ppkthdr = (struct pcap_pkthdr*)(buffer+need_write_len);
		ppkt_start = (char*)ppkthdr + PKT_HDR_LEN;
	}

	/* 抓包结束后如果还有没写入文件的报文则写入 */
	if(need_write_len > 0)
	{
		if(fwrite(buffer, sizeof(char), need_write_len, file) < need_write_len)
		{
			__android_log_print(ANDROID_LOG_INFO, "sprintwind", "fwrite failed");
			return NULL;
		}

		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "write unwritten data %d bytes\n", need_write_len);
	}


	fclose(file);

	__android_log_print(ANDROID_LOG_INFO, "sprintwind", "capture stopped, %d packets captured, saved to file %s\n", total_capture_count, file_name);

	/* 释放资源 */
	close(sock_fd);
	free(buffer);
	fclose(file);

	return NULL;
}

/*
 * 向stats_file中写入stats
 */
int write_statistics(char* stats_file, int stats)
{
	int writelen;
	char intArr[MAX_INTEGER_LEN] = {0};

	if(NULL == stats_file)
	{
		return -1;
	}

	FILE* file = fopen(stats_file, "w+");
	if(NULL == file)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "open statistics file %s failed, %s", stats_file, strerror(errno));
		return errno;
	}

	sprintf(intArr, "%d", stats);
	writelen = fwrite(intArr, sizeof(char), MAX_INTEGER_LEN, file);
	if(writelen <= 0)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind","write statistics fail, %s", strerror(errno));
		return errno;
	}

	fclose(file);
	return 0;
}

/*
 * 打印抓包状态线程
 */
void* print_thread(void* arg)
{
	char stats_file[FILE_NAME_LEN] = {0};

	sprintf(stats_file, "%s/%s", stats_file_dir, STATS_FILE);

	while(capture)
	{
		/* 将抓包个数写入统计文件 */
		if(0 != write_statistics(stats_file, total_capture_count))
		{
			break;
		}

		sleep(1);
	}

	/* 抓包结束后，将统计值写为0 */
	write_statistics(stats_file, 0);
	__android_log_print(ANDROID_LOG_INFO, "sprintwind","write statistics to 0");

	return NULL;
}

int start_capture(char* dev, int proto, int cap_len, char* saveDir, char* saveFileName)
{

	struct sock_fprog fprog;
	struct ifreq interface;
	struct sock_filter filter[] = {
					{ 0x28, 0, 0, 0x0000000c },
					{ 0x15, 0, 3, 0x00000800 },
					{ 0x30, 0, 0, 0x00000017 },
					{ 0x15, 0, 1, proto },
					{ 0x6, 0, 0, cap_len },
					{ 0x6, 0, 0, 0x00000000 }
				    };
	struct pcap_file_header file_header;

	sock_fd = socket(AF_PACKET, SOCK_RAW, htons(proto));
	if(sock_fd < 0)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "socket failed");
		return errno;
	}

	/* 输入了设备则进行绑定 */
	if(NULL != dev)
	{
		strncpy(interface.ifr_ifrn.ifrn_name, dev, IFNAMSIZ);
		if( setsockopt(sock_fd, SOL_SOCKET, SO_BINDTODEVICE, &interface, sizeof(interface)) < 0)
		{
			__android_log_print(ANDROID_LOG_INFO, "sprintwind", "SO_BINDTODEVICE failed");
			return errno;
		}
	}

	/* 设置过滤条件
	fprog.filter = filter;
	fprog.len = sizeof(filter)/sizeof(struct sock_filter);
	if( setsockopt(sock_fd, SOL_SOCKET, SO_ATTACH_FILTER, &fprog, sizeof(fprog)) < 0)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "SO_ATTACH_FILTER");
		return errno;
	}
	*/


	/* 初始化pcap文件头 */
	init_file_header(&file_header, cap_len);

	/* 生成文件名
	time(&time_now);
	struct tm* now = localtime(&time_now);
	now->tm_year += 1900;

	sprintf(file_name, "%s/%04d%02d%02d%02d%02d%02d.%s", saveDir, now->tm_year, now->tm_mon+1, now->tm_mday, now->tm_hour, now->tm_min, now->tm_sec, PCAP_FILE_SUFFIX);
	*/

	sprintf(file_name, "%s/%s.%s", saveDir, saveFileName, PCAP_FILE_SUFFIX);

	/* 创建pcap文件 */
	file = fopen(file_name, "w+");
	if(NULL == file)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "fopen:%s", file_name);
		return errno;
	}

	/* 写入文件头 */
	need_write_len = sizeof(struct pcap_file_header);
	if( fwrite(&file_header, sizeof(char), need_write_len, file) < need_write_len)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "fwrite");
		return errno;
	}

	__android_log_print(ANDROID_LOG_INFO, "sprintwind", "write file header %d bytes\n", need_write_len);

	/* 偏移写文件位置到文件头后面 */
	//fseek(file, need_write_len, SEEK_SET);

	/* 分配报文缓冲区 */
	buffer = (char*)malloc(BUFF_SIZE);
	if(NULL == buffer)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "alloc memory for buffer failed\n");
		return -1;
	}

	memset(buffer, 0, BUFF_SIZE);

	ppkthdr = (struct pcap_pkthdr*)buffer;
	ppkt_start = (char*)ppkthdr + PKT_HDR_LEN;

	capture_count = 0;
	total_capture_count = 0;
	need_write_len = 0;

	/*
	if(0 != pthread_create(&thread_id, NULL, capture_thread, NULL) )
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "create capture thread failed\n");
		return -1;
	}
	*/

	if(0 != pthread_create(&thread_id, NULL, print_thread, NULL))
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "create print thread failed\n");
		return -1;
	}


	capture_thread(NULL);

	return 0;

}

void stop_capture(int sig)
{
	__android_log_print(ANDROID_LOG_INFO, "sprintwind", "recv a signal, goto stop capture");
	capture = 0;
}

int get_protocol_value(char* proto)
{
	if(NULL == proto)
	{
		return -1;
	}

	if(0==strcmp(proto, "ARP"))
	{
		return ETH_P_ARP;
	}

	if(0==strcmp(proto, "IP"))
	{
		return ETH_P_IP;
	}

	if(0==strcmp(proto, "ALL"))
	{
		return ETH_P_ALL;
	}

	return -1;
}

int main(int argc, char* argv[])
{
	if(argc < 6)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "usage:%s <dev> <protocol> <cap_len> <save_path> <file_name>\n", argv[0]);
		return -1;
	}

	int proto = get_protocol_value(argv[2]);
	if(proto < 0)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "unsurpport protocol :%s\n", argv[2]);
		return -1;
	}

/*	pid_t pid = fork();
	if(pid < 0)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "fork failed, %s", strerror(errno));
		return -1;
	}*/

/*	if(pid == 0)
	{*/
		/* 保存传入的路径，用于创建统计文件 */
		strcpy(stats_file_dir, argv[4]);

		signal(SIGINT, stop_capture);
		signal(SIGTERM, stop_capture);
		signal(SIGKILL, stop_capture);

		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "in child process, pid:%d", getpid());
		if( start_capture(argv[1], proto, atoi(argv[3]), argv[4], argv[5]) != 0)
		{
			__android_log_print(ANDROID_LOG_INFO, "sprintwind", "start capture failed\n");
			return -1;
		}



		//capture_thread(NULL);
/*
	}
	else
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "in parent process, pid:%d", getpid());
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "capture started");
		exit(0);
	}
*/


	//__android_log_print(ANDROID_LOG_INFO, "sprintwind", "capture started, press ESC to stop\n");

	/*int ch;
	while((ch = getchar())!= EOF)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "input:%d\n", ch);
		if(ch == KEY_ESC)
		{
			stop_capture();
			__android_log_print(ANDROID_LOG_INFO, "sprintwind", "capture stopped, %d packets captured, saved to file %s\n", total_capture_count, file_name);
			break;
		}
	}*/

	return 0;
}


JNIEXPORT jint JNICALL Java_com_sprintwind_packetcapturetool_MainActivity_JNIstartCapture(JNIEnv* env, jobject obj, jstring dev, jint proto, jint cap_len)
//int start_capture(char* dev, int proto, int cap_len)
{

	struct sock_fprog fprog;
	struct ifreq interface;
	struct sock_filter filter[] = {
					{ 0x28, 0, 0, 0x0000000c },
					{ 0x15, 0, 3, 0x00000800 },
					{ 0x30, 0, 0, 0x00000017 },
					{ 0x15, 0, 1, proto },
					{ 0x6, 0, 0, cap_len },
					{ 0x6, 0, 0, 0x00000000 }
				    };
	struct pcap_file_header file_header;

	bool bIsCopy = false;

	//jint result = execl("/system/xbin/su", "su", NULL);
	//__android_log_print(ANDROID_LOG_INFO, "sprintwind", "result:%d", result);

	sock_fd = socket(AF_PACKET, SOCK_RAW, htons(ETH_P_ALL));
	if(sock_fd < 0)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "socket failed");
		return errno;
	}

	/* 输入了设备则进行绑定 */
	if(NULL != dev)
	{
		strncpy(interface.ifr_ifrn.ifrn_name, env->GetStringUTFChars(dev, NULL), IFNAMSIZ);
		if( setsockopt(sock_fd, SOL_SOCKET, SO_BINDTODEVICE, &interface, sizeof(interface)) < 0)
		{
			__android_log_print(ANDROID_LOG_INFO, "sprintwind", "SO_BINDTODEVICE");
			return errno;
		}
	}

	/* 设置过滤条件 */
	fprog.filter = filter;
	fprog.len = sizeof(filter)/sizeof(struct sock_filter);
	if( setsockopt(sock_fd, SOL_SOCKET, SO_ATTACH_FILTER, &fprog, sizeof(fprog)) < 0)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "SO_ATTACH_FILTER");
		return errno;
	}


	/* 初始化pcap文件头 */
	init_file_header(&file_header, cap_len);

	/* 生成文件名 */
	time(&time_now);
	struct tm* now = localtime(&time_now);
	now->tm_year += 1900;
	sprintf(file_name, "%04d%02d%02d%02d%02d%02d.%s", now->tm_year, now->tm_mon+1, now->tm_mday, now->tm_hour, now->tm_min, now->tm_sec, PCAP_FILE_SUFFIX);

	/* 创建pcap文件 */
	file = fopen(file_name, "w+");
	if(NULL == file)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "fopen");
		return errno;
	}

	/* 写入文件头 */
	need_write_len = sizeof(struct pcap_file_header);
	if( fwrite(&file_header, sizeof(char), need_write_len, file) < need_write_len)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "fwrite");
		return errno;
	}

	//printf("write file header %d bytes\n", need_write_len);

	/* 偏移写文件位置到文件头后面 */
	//fseek(file, need_write_len, SEEK_SET);

	/* 分配报文缓冲区 */
	buffer = (char*)malloc(BUFF_SIZE);
	if(NULL == buffer)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "alloc memory for buffer failed\n");
		return -1;
	}

	memset(buffer, 0, BUFF_SIZE);

	ppkthdr = (struct pcap_pkthdr*)buffer;
	ppkt_start = (char*)ppkthdr + PKT_HDR_LEN;

	capture_count = 0;
	need_write_len = 0;

	if(0 != pthread_create(&thread_id, NULL, capture_thread, NULL) )
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "create capture thread failed\n");
		return -1;
	}
}

JNIEXPORT void JNICALL Java_com_sprintwind_packetcapturetool_MainActivity_JNIstopCapture(JNIEnv* env, jobject obj)
//void stop_capture()
{
	capture = 0;
}

JNIEXPORT jint JNICALL Java_com_sprintwind_packetcapturetool_MainActivity_JNIgetProtoValue(JNIEnv* env, jobject obj, jstring protocol)
{
	jboolean isCopy = false;
	const char* proto = env->GetStringUTFChars(protocol, &isCopy);
	if(NULL == protocol)
	{
		return -1;
	}

	if(0==strcmp(proto, "TCP"))
	{
		return 6;
	}

	if(0==strcmp(proto, "UDP"))
	{
		return 17;
	}

	if(0==strcmp(proto, "ICMP"))
	{
		return 1;
	}

	return -1;
}

JNIEXPORT jstring JNICALL Java_com_sprintwind_packetcapturetool_MainActivity_JNIgetInterfaces(JNIEnv* env, jobject obj)
{
	int sock_fd;
	int if_len;
	struct ifconf ifc;
	struct ifreq buf[MAX_INTERFACE_NUM];//接口信息

	if((sock_fd = socket(AF_INET, SOCK_DGRAM, 0)) < 0)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "socket failed, %s", strerror(errno));
		return NULL;
	}

	ifc.ifc_len = sizeof(buf);
	ifc.ifc_buf = (caddr_t) buf;

	if (ioctl(sock_fd, SIOCGIFCONF, (char *) &ifc) == -1)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "ioctl failed");
		return NULL;
	}

	if_len = ifc.ifc_len / sizeof(struct ifreq);//接口数量

	if(if_len <= 0)
	{
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "if_len <= 0");
		return NULL;
	}

	__android_log_print(ANDROID_LOG_INFO, "sprintwind", "if_len:%d", if_len);

	char buff[1024] = {0};
	char* pResult = buff;

	int i = 0;
	for(; i<if_len; i++){
		__android_log_print(ANDROID_LOG_INFO, "sprintwind", "buf[%d]:%s", i, buf[i].ifr_name);
		if(0!=i)
		{
			*pResult = '|';
		}
		strcat(pResult, buf[i].ifr_name);
		pResult += strlen(buf[i].ifr_name);

	}

	pResult = buff;
	close(sock_fd);
	__android_log_print(ANDROID_LOG_INFO, "sprintwind", "return jstring");

	return env->NewStringUTF(pResult);
}

JNIEXPORT jint JNICALL Java_com_sprintwind_packetcapturetool_MainActivity_JNIexcuteCommand(JNIEnv* env, jobject obj, jstring cmd, jstring args)
{
	const char* strCmd = env->GetStringUTFChars(cmd, NULL);
	const char* strArgs = env->GetStringUTFChars(args, NULL);
	__android_log_print(ANDROID_LOG_INFO, "sprintwind", "strCmd:%s, strArgs:%s", strCmd, strArgs);

	if(-1 == execl(strCmd, strArgs)){
		return errno;
	}

	return 0;
}

JNIEXPORT jstring JNICALL Java_com_sprintwind_packetcapturetool_MainActivity_JNIgetErrorString(JNIEnv* env, jobject obj, jint err)
{
	return env->NewStringUTF(strerror(err));
}

JNIEXPORT jint JNICALL Java_com_sprintwind_packetcapturetool_MainActivity_JNIgetRootPermission(JNIEnv* env, jobject obj)
{
	//jint result = execl("/system/xbin/su", "su", NULL);
	jint result = system("su");
	__android_log_print(ANDROID_LOG_INFO, "sprintwind", "result:%d", result);
	return result;
}

#ifdef __cplusplus
}
#endif
