
extern int start_capture(char* dev, int proto, int cap_len);
extern void stop_capture();
extern int get_protocol_value(char* proto);

extern char* file_name;
extern int total_capture_count;
