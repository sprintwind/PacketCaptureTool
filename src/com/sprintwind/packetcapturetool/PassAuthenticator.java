package com.sprintwind.packetcapturetool;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class PassAuthenticator extends Authenticator{
	public PasswordAuthentication getPasswordAuthentication()  
    {  
        /**
         * 这个地方需要添加上自己的邮箱的账号和密码
         */
        String username = "624988625@qq.com";  
        String pwd = "lovesiyatou0913";  
        return new PasswordAuthentication(username, pwd);  
    }
}
