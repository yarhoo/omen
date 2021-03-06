package com.omen.netty.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.omen.netty.server.service.EventTriggerService;
import com.omen.netty.server.service.TcpServerService;
import com.omen.netty.utils.StringUtil;


@Sharable
@Service()@Scope("prototype")
public class TcpServerHandler extends ChannelHandlerAdapter {
	
	private static Logger log = Logger.getLogger(TcpServerHandler.class);
	
	@Autowired(required=false)
	private TcpServerService tcpServerService ;
	
	@Autowired(required=false)
	private EventTriggerService eventTriggerService ;
	
	
	@Override  
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {  
		if(eventTriggerService == null ){
			ctx.fireUserEventTriggered(evt);
		}else{
			eventTriggerService.userEventTriggered(ctx, evt);
		}
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx,Object msg)throws Exception{
//		log.debug("TCP请求，Initializer中创建handler hashCode:" + this.hashCode());
//		log.debug("omen收到TCP请求,开始读取数组数据...");
//		ByteBuf buf = (ByteBuf)msg;
//		byte[] req = new byte[buf.readableBytes()];
//		buf.readBytes(req);
		byte[] req = (byte[])msg;
		log.debug("读取数组数据完成,交付业务处理...");
		log.debug("TcpServerHandler read Bytes:" + StringUtil.parseByte2HexStr(req));
		byte[] returnMsg = tcpServerService.doService(ctx,req);
		log.debug("业务处理完成,准备返回结果...");
		if(returnMsg!=null){
			ByteBuf resp = Unpooled.copiedBuffer(returnMsg);
			ctx.writeAndFlush(resp);
		}
		log.debug("结果返回完成...");
			
	}
	
	 @Override
	  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		 if (ctx != null ) {  
			  log.error("Tcp 请求中发生异常,channelId[" +ctx.channel().id().asLongText() + "]", cause);
			  try {
				  log.info("开始关闭业务流程...");
				  tcpServerService.close(ctx,cause);
				} catch (Exception e) {
					log.error("无法正常关闭业务流程", e);
				}
			  log.info("开始检查channel是否存活...");
	          if (ctx.channel().isActive()) {  
	              ctx.close();  
	              log.info("channel存活,开始关闭...");
	          }else
	        	  log.info("channel已关闭,处理结束...");
	      }  
		  
	  }

}
