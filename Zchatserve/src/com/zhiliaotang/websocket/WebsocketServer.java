/**
 * 
 */

package com.zhiliaotang.websocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * Websocket的核心通信工具类，用于客户端和服务器之间的通讯功能
 * @author 余思翰
 *
 */

//Websocket规则：WS://host:port/contextroot/servername/param

@ServerEndpoint(value="/webchat/{param}")
public class WebsocketServer {

	/**创建连接时的方法，当用于请求到服务器时，首先处罚盖方法，改方法由客户端的onopen()进行触发
	 * @param param  用户请求路径中的参数
	 * @param session  用户的会话对象，一个用户一个会话对象
	 */
	
	private static final String GUEST_PREFIX = "zhiliaotang";//所有进入服务器的用户，我们都给他一个标识
	private Session session;//用户会话，存放所有的用户会话
	private final String nickname;//用户昵称，该数据有客户端传入（本次功能由系统生成）（以后可以由数据库或用户提供）
	
	//I++  ++I:并不是一种线程安全的行为，会产生线程问题
	private static final AtomicInteger connectionIds = new AtomicInteger(0);
	
	
	//用一个set存放所有的用户连接对象
	private static final Set<WebsocketServer> connections = new CopyOnWriteArraySet<>();
	
	//用一个Map，存放实时的用户列表
	private static final Map<String,String> usermap = new HashMap<String,String>();
	
	//构造函数，在类加载时，会执行该方法
		public WebsocketServer(){
			nickname = GUEST_PREFIX+connectionIds.getAndIncrement();
			//利用构造函数的特征，在类加载时，就为用户生成一个唯一的用户标识
			//zhiliaotangxxxx,后面的xxxx由connectionIds.getAndIncrement()提供
		}
	
	@OnOpen
	public void onOpen(@PathParam(value="param") String param,Session session){
		System.out.println("用户上线了：" + param);
		System.out.println("上线的用户是：" + this.nickname);
		this.session = session;//讲用户Session交给系统管理
		connections.add(this);//将当前的用户域对象加入到Set中统一管理
		//**这里需要将用户的昵称加入到map中
		usermap.put(this.nickname, getInfo(param)[1]);
	
		String message = String.format("* %s %s",getInfo(param)[1],"上线了");//* xxx 上线了 （第一位为*号的为系统消息）
		//调用群发功能
		sendToAll(message);
		
		//群发用户
		sendOnLineList();
		
	}
	
	//
	private static void sendOnLineList() {
		// TODO Auto-generated method stub
		
		String onlinelist = "";
		for(Map.Entry<String, String> entry:usermap.entrySet()){
			String key = entry.getKey().toString();
			String value = entry.getValue().toString();
			onlinelist +=","+key+"&!&"+value;
		}
		String m = String.format("* %s %s","当前在线用户",onlinelist);
		System.out.println(m);
		sendToAll(m);
	}

	public static void sendToAll(String message) {
		// TODO Auto-generated method stub
		for(WebsocketServer clinet: connections){
			synchronized (clinet) {
				try {
					//获得基础网关，向客户端发送消息：消息为文本消息
					clinet.session.getBasicRemote().sendText(message);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println("异常：向客户端发送消息失败");
					try {
						clinet.session.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * 解析数据，并获得数据组
	 * @param vmsg
	 * @return
	 */
	public static String[] getInfo(String vmsg){
		String p[] = vmsg.split("&A&");
		return p;
	}
	
	
	
	/**
	 * 
	 * @param param  用户请求路径中的参数
	 */
	@OnClose
	public void onClose(@PathParam(value="param") String param){
		System.out.println("用户离线了：" + param);
		connections.remove(this);//移除用户
		usermap.remove(this.nickname);
		String message = String.format("* %s %s", getInfo(param)[1],"下线了");
		sendToAll(message);
		sendOnLineList();
	}
	
	/**
	 * 
	 * @param param  用户请求路径中的参数
	 * @param message
	 */
	@OnMessage
	public void onMessage(@PathParam(value="param") String param,String message){
		System.out.println(getInfo(param)[1]+":"+message);
		//数据格式化 nickname&@&message
		String m = String.format("%s%s",getInfo(param)[1]+"&@&",message);
		sendToAll(m);
	}
	
	/**
	 * 
	 * @param param  用户请求路径中的参数
	 * @param t
	 */
	@OnError
	public void onError(@PathParam(value="param") String param,Throwable t){
		
	}
}
