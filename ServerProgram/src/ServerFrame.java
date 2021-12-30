

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.SwingConstants;


public class ServerFrame extends JFrame {

	private JTextArea textArea;
	private JTextField msgField;
	private JButton sendButton;
	private ServerSocket serverSocket;
	private Socket socket;
	private DataInputStream reader;
	private DataOutputStream writer;
	private JButton exitButton;
	private JLabel nickLabel;
	private JPanel menuPanel;

	public ServerFrame(int port) throws IOException {
		// 소켓 연결
		serverSocket = new ServerSocket(port); // 소켓 부분 익셉션 메인 프레임에서 해줌

		/* 디자인 */
		setBackground(Color.WHITE);

		setTitle("Server");
		setBackground(new Color(255, 255, 255));
		setBounds(450, 50, 345, 450);
		getContentPane().setLayout(null);

		textArea = new JTextArea();
		textArea.setBackground(new Color(176, 196, 222));

		textArea.setEditable(false); // 쓰기 금지

		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setBounds(0, 36, 345, 342);

		getContentPane().add(scrollPane);

		JPanel msgPanel = new JPanel();
		msgPanel.setBounds(0, 378, 345, 45);

		msgPanel.setLayout(new BorderLayout());
		textArea.append("기다리는 중...");

		msgField = new JTextField();

		sendButton = new JButton("send");

		msgPanel.add(msgField, BorderLayout.CENTER);

		msgPanel.add(sendButton, BorderLayout.EAST);

		getContentPane().add(msgPanel);

		menuPanel = new JPanel();
		menuPanel.setBackground(new Color(240, 248, 255));
		menuPanel.setBounds(0, 0, 345, 36);
		getContentPane().add(menuPanel);
		menuPanel.setLayout(null);

		exitButton = new JButton("채팅 종료");
		exitButton.setBounds(237, 6, 102, 29);
		menuPanel.add(exitButton);

		nickLabel = new JLabel("<대화상대: 클라이언트>");
		nickLabel.setHorizontalAlignment(SwingConstants.LEFT);
		nickLabel.setBounds(6, 6, 219, 21);
		menuPanel.add(nickLabel);
		// 종료버튼 리스너
		exitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int exitOption = JOptionPane.showConfirmDialog(null, "종료하시겠습니까?", "채팅 종료", JOptionPane.YES_NO_OPTION);
				// YES_OPTION은 0, NO_OPTION은 1, CLOSED_OPTION은 -1을 반환한다
				if (exitOption == JOptionPane.YES_OPTION) {
					try {

						if (writer != null)
							writer.close();

						if (reader != null)
							reader.close();

						if (socket != null)
							socket.close();

						if (serverSocket != null)
							serverSocket.close();

					} catch (IOException e1) {
						e1.printStackTrace();
					}
					dispose();

				} else if ((exitOption == JOptionPane.NO_OPTION) || (exitOption == JOptionPane.CLOSED_OPTION)) {
					return; 
				}
			}
		});

		// 엔터키 리스너

		msgField.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				super.keyPressed(e);
				int keyCode = e.getKeyCode();
				switch (keyCode) {
				case KeyEvent.VK_ENTER:
					sendMessage();
					break;
				}
			}
		});

		msgField.requestFocus();
		

		
		// 스레드 
		ReceiverThread serverThread = new ReceiverThread();
		serverThread.setDaemon(true); 
		serverThread.start();

		// send 버튼 리스너
		sendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendMessage(); 
			}
		});

		setVisible(true); // 보이기

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				try {

					if (writer != null)
						writer.close();

					if (reader != null)
						reader.close();

					if (socket != null)
						socket.close();

					if (serverSocket != null)
						serverSocket.close();

				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

	}// 생성자 메소드

	
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("(HH:mm:ss)");

	// 이너클래스
	class ReceiverThread extends Thread {
		
		public void run() {

			try { // 서버 소켓 생성 작업
				socket = serverSocket.accept();// 클라이언트가 접속할때까지 커서(스레드)가 대기
				textArea.append("대화를 시작합니다.\n");

				// 통신하는 스트림
				reader = new DataInputStream(socket.getInputStream());
				writer = new DataOutputStream(socket.getOutputStream());

				while (true) {
					LocalTime now = LocalTime.now();
					String time = now.format(formatter);

					// 상대방이 보내온 데이터를 읽기
					String msg = reader.readUTF();
					textArea.append(" [클라이언트] : " + msg + " " + time + "\n"); // 화면에 쓰기
					textArea.setCaretPosition(textArea.getText().length());
				}
			} catch (IOException e) {
				textArea.append("대화가 종료되었습니다.\n");
			}

		}

	}

	// 메시지 전송하는 기능 메소드

	void sendMessage() {
		LocalTime now = LocalTime.now();
		String time = now.format(formatter);

		String msg = msgField.getText(); // TextField에 써있는 글씨를 얻어오기
		msgField.setText(""); // 입력 후 빈칸으로
		textArea.append(" [서버] : " + msg + " " + time + "\n");// 1.TextArea(채팅창)에 표시
		textArea.setCaretPosition(textArea.getText().length()); // 스크롤 따라가게

		// 수신 스레드

		Thread senderThread = new Thread() {

			public void run() {
				try {
					writer.writeUTF(msg); // 쓰기
					writer.flush();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, "전송을 실패하였습니다.", "message", JOptionPane.PLAIN_MESSAGE);
				}
			}
		};
		senderThread.start();

	}
	
	public static void main(String[] args) throws IOException {
		new ServerFrame(9976);
	}

}// class