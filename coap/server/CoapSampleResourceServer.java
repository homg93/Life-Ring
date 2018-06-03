package org.ws4d.coap.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.ws4d.coap.connection.BasicCoapClientChannel;
import org.ws4d.coap.connection.BasicCoapServerChannel;
import org.ws4d.coap.connection.BasicCoapSocketHandler;
import org.ws4d.coap.interfaces.CoapChannel;
import org.ws4d.coap.interfaces.CoapClientChannel;
import org.ws4d.coap.interfaces.CoapResponse;
import org.ws4d.coap.messages.CoapMediaType;
import org.ws4d.coap.rest.BasicCoapResource;
import org.ws4d.coap.rest.CoapResourceServer;
import org.ws4d.coap.rest.ResourceHandler;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiBcmPin;

import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;


/**
 * @author Christian Lerche <christian.lerche@uni-rostock.de>
 * 
 */
public class CoapSampleResourceServer 
{

	private static CoapSampleResourceServer sampleServer;
	private CoapResourceServer resourceServer;
	private static Logger logger = Logger
			.getLogger(CoapSampleResourceServer.class.getName());
	
	//GPIO
	private GpioController gpio;
	private GpioPinDigitalOutput RedLedControlPin, GreenLedControlPin, BlueLedControlPin;
	private GpioPinDigitalInput VCS, RS; //VCS 진동감지센서 RS 적외선센서
	
	static int nonRS_Input = 0; //착용여부 판별 변수
	
	public void gpioInit()
	{
		gpio =GpioFactory.getInstance();
		RedLedControlPin = gpio.provisionDigitalOutputPin(RaspiBcmPin.GPIO_23);
		GreenLedControlPin = gpio.provisionDigitalOutputPin(RaspiBcmPin.GPIO_24);
		BlueLedControlPin = gpio.provisionDigitalOutputPin(RaspiBcmPin.GPIO_25);
		
		RedLedControlPin.setShutdownOptions(true, PinState.LOW);
		GreenLedControlPin.setShutdownOptions(true, PinState.LOW);
		BlueLedControlPin.setShutdownOptions(true, PinState.LOW);
		RedLedControlPin.low();
		GreenLedControlPin.low();
		BlueLedControlPin.low();
	}
	
	public void gpioVi(){//진동센서 GPIO설정
		gpio = GpioFactory.getInstance();
		VCS = gpio.provisionDigitalInputPin(RaspiBcmPin.GPIO_22);
		VCS.setShutdownOptions(true, PinState.LOW);
		gpio.getState(VCS);
	}
	public void gpioRS(){//적외선 센서 GPIO설정
		gpio = GpioFactory.getInstance();
		RS = gpio.provisionDigitalInputPin(RaspiBcmPin.GPIO_06);
		RS.setShutdownOptions(true, PinState.LOW);
		gpio.getState(RS);	
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
        logger.addAppender(new ConsoleAppender(new SimpleLayout()));
		logger.setLevel(Level.INFO);
		logger.info("Start Sample Resource Server");
		sampleServer = new CoapSampleResourceServer();
		
		sampleServer.gpioInit();
		sampleServer.gpioVi();
		sampleServer.gpioRS();
		sampleServer.run();
		
	}
	
	private void run() throws IOException 
	{
		if (resourceServer != null)
			resourceServer.stop();
		resourceServer = new CoapResourceServer();
		
		/* Show detailed logging of Resource Server*/
		Logger resourceLogger = Logger.getLogger(CoapResourceServer.class.getName());
		resourceLogger.setLevel(Level.ALL);
		
		/* add resources */
		// LifeRing리소스 추가 
		final BasicCoapResource LifeRing = new BasicCoapResource("/test/LifeRing", 
							"LifeRing Actuator".getBytes(), CoapMediaType.text_plain);
		LifeRing.registerResourceHandler(new ResourceHandler() {
			@Override
			public void onPost(byte[] data) {
				System.out.println("Post to /test/LifeRing");
					String inputData = new String(data);
					int lowcnt =0, highcnt = 0; //맥박이 안 뛸 때, 맥박이 뛸 때
					int cnt =0; //시간초를 재는 변수
					int i=0, i2=0; // 반복문 변수들
					int [] big_data; // 분당 맥박수를 기록하는 배열
					big_data = new int[100];

				if(inputData.equals("resetData")){
					nonRS_Input = 0; // 반지 착용여부 초기화
				}
				try {
					if(inputData.equals("LRon"))//LifeRing On
					{
						System.out.println("센서 작동");
						while(nonRS_Input < 30){//반지 착용한 상태일때 반복문
							if(cnt % 20 == 0){//10초마다 맥박 수 확인
								System.out.println("시간초 : " + cnt/2 + " 맥박수 :" + highcnt);
							}
							if(cnt >= 120){//1분마다 체크
								//위의 반복문을 약 0.5초마다 반복하기 때문에 cnt/2를 60초라고 가정하였습니다.
								big_data[i2] =highcnt;//빅데이터에 맥박 수 저장
								System.out.println(i2+1 +"번째 분당 맥박 수 : "+ big_data[i2]);
								highcnt=0;
								cnt =0;
								i2++;
							}
							cnt++; //시간초 증가
							if (gpio.isLow(RS))
							{
								BlueLedControlPin.low();
								//System.out.println("nonRS_Input : " + nonRS_Input);
								Thread.sleep(400);
								nonRS_Input++;//적외선 센서가 low이라면 변수 증가
								//이 변수가 30이상되면 반지를 착용하지 않은 것으로 판별
							}
							if(gpio.isHigh(RS)){
								nonRS_Input = 0;//반지 착용중이라면 변수를 0으로
								System.out.println("반지 착용중");
								BlueLedControlPin.high(); //착용한 상태 파란불 온
							}
							if(gpio.isHigh(VCS))//진동 감지 !
								{
									System.out.println("VCS IS HIGH !!!!!!!");
									lowcnt = 0; //맥박은 뛰고 있는것으로 판별
									highcnt +=2;// 맥박 수 증가
									
									GreenLedControlPin.high();
									Thread.sleep(500);
									GreenLedControlPin.low();
									
									if(cnt > 30){//15초 이상일 때, 맥박 데이터를 기반으로 심장 발작 판단
										if(highcnt >= cnt*1.35){//비율상으로 1분에 162회 이상 맥박이 뛴다면
											// 맥박이 시간에 비해 너무 빨리 뛴다면 심장발작으로 판단
											System.out.println("구급차 부르기 !!!!! *심장발작 !! *심근경색 위험 !!");
											RedLedControlPin.high();
											Thread.sleep(500);
											RedLedControlPin.low();
											Thread.sleep(300);
										}
									}
								}
							else if(gpio.isLow(VCS))//진동이 감지가 안된다면 변수 증가
								{
									System.out.println("VCS IS LOW : " + lowcnt);
									Thread.sleep(400);
									lowcnt++;
									if(lowcnt>20){//20번 연속으로 맥박이 감지가 안된다면 심장마비로 판단 
										System.out.println("구급차 부르기 !!!!! ***심장마비***");
										System.out.println("**************사이렌***************");
										RedLedControlPin.high();
										Thread.sleep(500);
										RedLedControlPin.low();
										Thread.sleep(300);
								}
							}
						}
						System.out.println("반지를 착용하지 않았습니다.");	
					}
			}catch (InterruptedException e) {
				e.printStackTrace();
			}
				
					if(inputData.equals("LRoff")){
						System.out.println("센서 종료");
						//VCS.clearProperties();
						return;
					}
					
					/* 적외선 센서 동작 확인 코드
					if(inputData.equals("RSon"))
					{
						while(true){

							if(gpio.isHigh(RS)){
								i++;
								System.out.println(i);
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}	
							}
							else if(gpio.isLow(RS)){
								BlueLedControlPin.low();
								System.out.println("반지를 착용하지 않았습니다.");
								
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}*/
					
				
					if(inputData.equals("redOn"))
					{
						RedLedControlPin.high();
						System.out.println("센서 작동");
					}
					else if(inputData.equals("redOff"))
					{
						RedLedControlPin.low();
					}
					
					if(inputData.equals("greenOn"))
					{
						GreenLedControlPin.high();
					}
					else if(inputData.equals("greenOff"))
					{
						GreenLedControlPin.low();
					}
					
					if(inputData.equals("blueOn"))
					{
						BlueLedControlPin.high();
					}
					else if(inputData.equals("blueOff"))
					{
						BlueLedControlPin.low();
					}
					
					if(inputData.equals("allOn"))
					{
						RedLedControlPin.high();
						GreenLedControlPin.high();
						BlueLedControlPin.high();
					}
					else if(inputData.equals("allOff"))
					{
						RedLedControlPin.low();
						GreenLedControlPin.low();
						BlueLedControlPin.low();
					}	
			}
		});
		LifeRing.setObservable(false);
		resourceServer.createResource(LifeRing);
		try {
			resourceServer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		}
		//observe example
		
		int counter = 0;
		/*while(true){
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			counter++;
			light.setValue(((String)"Message #" + counter).getBytes());
			light.changed();
		}
		*/
	}

