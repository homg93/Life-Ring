package org.ws4d.coap.client;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.server.ServerCloneException;
import java.util.Random;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.ws4d.coap.Constants;
import org.ws4d.coap.connection.BasicCoapChannelManager;
import org.ws4d.coap.connection.BasicCoapClientChannel;
import org.ws4d.coap.interfaces.CoapChannelManager;
import org.ws4d.coap.interfaces.CoapClient;
import org.ws4d.coap.interfaces.CoapClientChannel;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapResponse;
import org.ws4d.coap.messages.CoapBlockOption;
import org.ws4d.coap.messages.CoapBlockOption.CoapBlockSize;
import org.ws4d.coap.messages.CoapMediaType;
import org.ws4d.coap.messages.CoapRequestCode;

/**
 * @author	Christian Lerche <christian.lerche@uni-rostock.de>
 * 			Bjoern Konieczek <bjoern.konieczek@uni-rostock.de>
 */
public class BasicCoapClient extends JFrame implements CoapClient, ActionListener
{
	private String SERVER_ADDRESS;
	private int PORT; 

	static int counter = 0;
	private CoapChannelManager channelManager = null;
	private BasicCoapClientChannel clientChannel = null;
	private Random tokenGen = null;


	//UI
	private JTextField uriField;
	private JTextField payloadField;
	private JButton postBtn, getBtn;
	private JTextArea area;

	public BasicCoapClient(String server_addr, int port ){
		super();
		this.SERVER_ADDRESS = server_addr;
		this.PORT = port;
		this.channelManager = BasicCoapChannelManager.getInstance();
		this.tokenGen = new Random();



	}
	public boolean connect(){
		try {
			clientChannel = (BasicCoapClientChannel) channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
		} catch( UnknownHostException e ){
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean connect( String server_addr, int port ){
		this.SERVER_ADDRESS = server_addr;
		this.PORT = port;
		return this.connect();
	}


	public CoapRequest createRequest( boolean reliable, CoapRequestCode reqCode ) {
		return clientChannel.createRequest( reliable, reqCode );
	}

	public byte[] generateRequestToken(int tokenLength ){
		byte[] token = new byte[tokenLength];
		tokenGen.nextBytes(token);
		return token;
	}

	@Override
	public void onConnectionFailed(CoapClientChannel channel, boolean notReachable, boolean resetByServer) {
		System.out.println("Connection Failed");
	}

	@Override
	public void onResponse(CoapClientChannel channel, CoapResponse response) { //메시지가 여기로 들어옴
		System.out.println("Received response");
		if(response.getPayload() != null)
		{
			String responseData = new String(response.getPayload()); 
			System.out.println(responseData);
			area.append("Response:\n");
			if(responseData.matches(".*well-known/core.*")) //matches .* abc .*하면 문구중에 abc있는걸 출력 
			{
				String[] tempData = responseData.split(",");
				for(String data : tempData)
				{
					if(!data.equals("</.well-known/core>"))
					{
						area.append(data+"\n");
					}
				}
			}else
			{
				area.append(responseData+"\n");
			}
			area.append("--------------------------------\n");

		}else
		{
			System.out.println("response payload null");
			area.append("Response:\n");
			area.append("response payload null\n");
			area.append("--------------------------------\n");
		}
	}

	@Override
	public void onMCResponse(CoapClientChannel channel, CoapResponse response, InetAddress srcAddress, int srcPort) {
		System.out.println("MCReceived response");
	}

	public void resourceDiscoveryExample()
	{
		try {
			clientChannel = (BasicCoapClientChannel) channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
			CoapRequest coapRequest = clientChannel.createRequest(true, CoapRequestCode.GET);
			byte [] token = generateRequestToken(3);
			coapRequest.setUriPath("/.well-known/core");
			coapRequest.setToken(token);
			clientChannel.sendMessage(coapRequest);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public void postExample()
	{
		try {
			clientChannel = (BasicCoapClientChannel) channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
			CoapRequest coapRequest = clientChannel.createRequest(true, CoapRequestCode.POST);
			byte [] token = generateRequestToken(3);
			coapRequest.setUriPath("/test/light");
			coapRequest.setToken(token);
			coapRequest.setPayload("lightONONONONON!!!!".getBytes());
			clientChannel.sendMessage(coapRequest);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public void getExample()
	{
		try {
			clientChannel = (BasicCoapClientChannel) channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
			CoapRequest coapRequest = clientChannel.createRequest(true, CoapRequestCode.GET);
			byte [] token = generateRequestToken(3);
			coapRequest.setUriPath("/test/light");
			coapRequest.setToken(token);
			clientChannel.sendMessage(coapRequest);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public void observeExample()
	{
		try {
			clientChannel = (BasicCoapClientChannel) channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
			CoapRequest coapRequest = clientChannel.createRequest(true, CoapRequestCode.GET);
			byte [] token = generateRequestToken(3);
			coapRequest.setUriPath("/test/light");
			coapRequest.setToken(token);
			coapRequest.setObserveOption(1);  
			clientChannel.sendMessage(coapRequest);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}

	//client Ui
	public void clientUi()
	{
		setTitle("CoAP_Project Life Ring");
		setSize(600,380);
		setLocation(400,300);
		setLayout(null);
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JLabel label = new JLabel("URI Path");
		label.setBounds(10, 20, 100, 30);
		label.setFont(new Font("Serif", Font.PLAIN, 15));
		add(label);

		uriField = new JTextField();
		uriField.setBounds(100, 20, 150, 30);
		uriField.setFont(new Font("Serif", Font.PLAIN, 15));
		add(uriField);

		label = new JLabel("Payload");
		label.setBounds(10, 70, 100, 30);
		label.setFont(new Font("Serif", Font.PLAIN, 15));
		add(label);

		payloadField = new JTextField();
		payloadField.setBounds(100, 70, 150, 30);
		payloadField.setFont(new Font("Serif", Font.PLAIN, 15));
		add(payloadField);

		getBtn = new JButton("GET");
		getBtn.setBounds(30, 130, 100, 30);
		getBtn.setFont(new Font("Serif", Font.PLAIN, 15));
		getBtn.addActionListener(this);
		add(getBtn);

		postBtn = new JButton("POST");
		postBtn.setBounds(140, 130, 100, 30);
		postBtn.setFont(new Font("Serif", Font.PLAIN, 15));
		postBtn.addActionListener(this);
		add(postBtn);

		area = new JTextArea();
		area.setFont(new Font("Serif", Font.PLAIN, 15));
		JScrollPane sc = new JScrollPane(area);
		sc.setBounds(270, 20, 300, 300);
		add(sc);


		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(e.getSource() == getBtn)
		{
			String uriPath= uriField.getText();

			area.append("Request:\n");
			area.append("Method: GET\n");
			area.append("Uri path: "+uriPath+"\n");
			area.append("--------------------------------\n");
			if(uriPath.equals(".well-known/core"))
			{
				area.append("**Resource Discovery**\n");
			}else
			{
				
			}
			try {
				clientChannel = (BasicCoapClientChannel) channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
				CoapRequest coapRequest = clientChannel.createRequest(true, CoapRequestCode.GET);
				byte [] token = generateRequestToken(3);
				coapRequest.setUriPath(uriPath);
				coapRequest.setToken(token);
				clientChannel.sendMessage(coapRequest);

			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else if(e.getSource() == postBtn)
		{
			String uriPath= uriField.getText();
			String payload = payloadField.getText();
			area.append("Request:\n");
			area.append("Method: POST\n");
			area.append("Uri path: "+uriPath+"\n");
			area.append("Payload: "+payload+"\n");
			area.append("--------------------------------\n");
			try {
				clientChannel = (BasicCoapClientChannel) channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
				CoapRequest coapRequest = clientChannel.createRequest(true, CoapRequestCode.POST);
				byte [] token = generateRequestToken(3);
				coapRequest.setUriPath(uriPath);
				coapRequest.setPayload(payload.getBytes());
				coapRequest.setToken(token);
				clientChannel.sendMessage(coapRequest);

			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	public static void main(String[] args)
	{
		System.out.println("Start CoAP Client");
		String serverIp = "192.168.7.10";
		//Constants.COAP_DEFAULT_PORT (5683)
		BasicCoapClient client = new BasicCoapClient(serverIp, Constants.COAP_DEFAULT_PORT);//CoAP에서 사용하는 포트번호
		client.channelManager = BasicCoapChannelManager.getInstance();
		
		//UI
		client.clientUi();

		//example
		//client.resourceDiscoveryExample();
		//client.getExample();
		//client.postExample();
		//client.observeExample();
	}

}
