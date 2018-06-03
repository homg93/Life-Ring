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
	private GpioPinDigitalInput VCS, RS; //VCS ������������ RS ���ܼ�����
	
	static int nonRS_Input = 0; //���뿩�� �Ǻ� ����
	
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
	
	public void gpioVi(){//�������� GPIO����
		gpio = GpioFactory.getInstance();
		VCS = gpio.provisionDigitalInputPin(RaspiBcmPin.GPIO_22);
		VCS.setShutdownOptions(true, PinState.LOW);
		gpio.getState(VCS);
	}
	public void gpioRS(){//���ܼ� ���� GPIO����
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
		// LifeRing���ҽ� �߰� 
		final BasicCoapResource LifeRing = new BasicCoapResource("/test/LifeRing", 
							"LifeRing Actuator".getBytes(), CoapMediaType.text_plain);
		LifeRing.registerResourceHandler(new ResourceHandler() {
			@Override
			public void onPost(byte[] data) {
				System.out.println("Post to /test/LifeRing");
					String inputData = new String(data);
					int lowcnt =0, highcnt = 0; //�ƹ��� �� �� ��, �ƹ��� �� ��
					int cnt =0; //�ð��ʸ� ��� ����
					int i=0, i2=0; // �ݺ��� ������
					int [] big_data; // �д� �ƹڼ��� ����ϴ� �迭
					big_data = new int[100];

				if(inputData.equals("resetData")){
					nonRS_Input = 0; // ���� ���뿩�� �ʱ�ȭ
				}
				try {
					if(inputData.equals("LRon"))//LifeRing On
					{
						System.out.println("���� �۵�");
						while(nonRS_Input < 30){//���� ������ �����϶� �ݺ���
							if(cnt % 20 == 0){//10�ʸ��� �ƹ� �� Ȯ��
								System.out.println("�ð��� : " + cnt/2 + " �ƹڼ� :" + highcnt);
							}
							if(cnt >= 120){//1�и��� üũ
								//���� �ݺ����� �� 0.5�ʸ��� �ݺ��ϱ� ������ cnt/2�� 60�ʶ�� �����Ͽ����ϴ�.
								big_data[i2] =highcnt;//�����Ϳ� �ƹ� �� ����
								System.out.println(i2+1 +"��° �д� �ƹ� �� : "+ big_data[i2]);
								highcnt=0;
								cnt =0;
								i2++;
							}
							cnt++; //�ð��� ����
							if (gpio.isLow(RS))
							{
								BlueLedControlPin.low();
								//System.out.println("nonRS_Input : " + nonRS_Input);
								Thread.sleep(400);
								nonRS_Input++;//���ܼ� ������ low�̶�� ���� ����
								//�� ������ 30�̻�Ǹ� ������ �������� ���� ������ �Ǻ�
							}
							if(gpio.isHigh(RS)){
								nonRS_Input = 0;//���� �������̶�� ������ 0����
								System.out.println("���� ������");
								BlueLedControlPin.high(); //������ ���� �Ķ��� ��
							}
							if(gpio.isHigh(VCS))//���� ���� !
								{
									System.out.println("VCS IS HIGH !!!!!!!");
									lowcnt = 0; //�ƹ��� �ٰ� �ִ°����� �Ǻ�
									highcnt +=2;// �ƹ� �� ����
									
									GreenLedControlPin.high();
									Thread.sleep(500);
									GreenLedControlPin.low();
									
									if(cnt > 30){//15�� �̻��� ��, �ƹ� �����͸� ������� ���� ���� �Ǵ�
										if(highcnt >= cnt*1.35){//���������� 1�п� 162ȸ �̻� �ƹ��� �ڴٸ�
											// �ƹ��� �ð��� ���� �ʹ� ���� �ڴٸ� ����������� �Ǵ�
											System.out.println("������ �θ��� !!!!! *������� !! *�ɱٰ�� ���� !!");
											RedLedControlPin.high();
											Thread.sleep(500);
											RedLedControlPin.low();
											Thread.sleep(300);
										}
									}
								}
							else if(gpio.isLow(VCS))//������ ������ �ȵȴٸ� ���� ����
								{
									System.out.println("VCS IS LOW : " + lowcnt);
									Thread.sleep(400);
									lowcnt++;
									if(lowcnt>20){//20�� �������� �ƹ��� ������ �ȵȴٸ� ���帶��� �Ǵ� 
										System.out.println("������ �θ��� !!!!! ***���帶��***");
										System.out.println("**************���̷�***************");
										RedLedControlPin.high();
										Thread.sleep(500);
										RedLedControlPin.low();
										Thread.sleep(300);
								}
							}
						}
						System.out.println("������ �������� �ʾҽ��ϴ�.");	
					}
			}catch (InterruptedException e) {
				e.printStackTrace();
			}
				
					if(inputData.equals("LRoff")){
						System.out.println("���� ����");
						//VCS.clearProperties();
						return;
					}
					
					/* ���ܼ� ���� ���� Ȯ�� �ڵ�
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
								System.out.println("������ �������� �ʾҽ��ϴ�.");
								
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
						System.out.println("���� �۵�");
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

