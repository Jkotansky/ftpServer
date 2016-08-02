
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

public class FTPServer {
	final static String CRLF = "\r\n";
	static String IP = "";
	static int portNumber = 0;
	static int userCount;
	static int passCount;
	static boolean user;
	static boolean pass;
	static boolean portNum;
	static boolean quit;
	static BufferedReader inFromClient;
	static DataOutputStream outToClient;
	static Socket transfer, connectionSocket;
	static boolean connectionStatus;
	
	public static void main(String[] args) throws Exception {
		user = false;
		pass = false;
		portNum = false;
		quit = false;
		passCount = 0;
		userCount = 0;


		// Create "welcoming" socket using port 9000
		ServerSocket welcomeSocket = new ServerSocket(Integer.parseInt(args[0]));

		while(true){
			//wait for connection
			connectionSocket = welcomeSocket.accept();
			
			connectionStatus = true;
					
			System.out.println("220 COMP 431 FTP server ready.");
			//Create (buffered) Input stream attached to connection socket
			inFromClient = new BufferedReader(
				new InputStreamReader(connectionSocket.getInputStream()));
	
			// Create output stream attached to connection socket
			outToClient = new DataOutputStream(
							connectionSocket.getOutputStream());
					
			push("220 COMP 431 FTP server ready.");		
			
			while(connectionStatus == true){
			
				String message;
				
				if((message = inFromClient.readLine())!= null){
				
					Scanner sc = new Scanner(message);
			
					// using the built in delimiter method to get ride of all of the CRLF as they come in
					sc.useDelimiter(CRLF);
			
					while (sc.hasNext() && quit == false) {
			
						String input = sc.next();
						String s = "";
			
						//this for loop goes through and reads every inputed string and if it has any invalid characters
						// it goes ahead and throws the error code for CRLF missing from the command
						// it also prints out the string if it is invalid
						
						for (int i = 0; i < input.length(); i++) {
							char c = input.charAt(i);
							if (c == '\r' || c == '\n') {
								System.out.print(s + c);
								System.out.println("501 Syntax error in parameter.");
								push("501 Syntax error in parameter.");
								s = "";
							} else {
								s += c;
							}
						}
			
						//prints out the inputed string and parses the command
						System.out.print(s + CRLF);
						parse(s);
							}//end while
					
				}else{
					connectionStatus = false;
				}
			}//end inner server while
		}//end server while
	}

	static void parse(String inputline) throws Exception {
		ArrayList<String> tokens = new ArrayList<String>();
		StringTokenizer tokenizedLine = new StringTokenizer(inputline);
		
		//adds every token into the ArrayList to be parsed.
		while (tokenizedLine.hasMoreTokens()) {
			tokens.add(tokenizedLine.nextToken());
		}

		String command = tokens.get(0);
		String parameter;
		
		//Safety check to make sure there's not an error and that the command is good to go.
		if(isValid(command)){

			//this sequence goes through to check if the command is valid first
		if (command.equalsIgnoreCase("SYST")
				|| command.equalsIgnoreCase("NOOP")
				|| command.equalsIgnoreCase("QUIT")) {
			// by checking if the token array has any more elements we can determine if there is an error
			//if it had more tokens this would be a parameter error and would throw the CRLF error
			if (tokens.size() > 1) {
				System.out.println("501 Syntax error in parameter.");
			} else {
				if (command.equalsIgnoreCase("SYST")) {
					System.out.println("215 UNIX Type: L8.");
					push("215 UNIX Type: L8.");
				}else if(command.equalsIgnoreCase("QUIT")){
					System.out.println("221 Goodbye.");
					push("221 Goodbye.");
					connectionStatus = false;
					outToClient.close();
					inFromClient.close();
					connectionSocket.close();
					quit = false;
				} else {
					System.out.println("200 Command OK.");
					push("200 Command OK.");
				}
			}

		} else if (command.equalsIgnoreCase("TYPE")) {
			//this checks to see if the command has an extra space amended to it
			if (inputline.length() == 4) {
				System.out.println("500 Syntax error, command unrecognized.");
				push("500 Syntax error, command unrecognized.");
			} else {
				//if the size of the array isn't two than it doesn't follow the parameter
				//this falls into a checkpoint which determines if it has one of the correct types
				if (tokens.size() == 2) {
					parameter = tokens.get(1);
					if (parameter.equals("A") || parameter.equals("I")) {
						if (parameter.equals("A")) {
							System.out.println("200 Type set to A.");
							push("200 Type set to A.");
						} else {
							System.out.println("200 Type set to I.");
							push("200 Type set to I.");
						}
					} else {
						System.out.println("501 Syntax error in parameter.");
						push("501 Syntax error in parameter.");
					}
				} else {
					System.out.println("501 Syntax error in parameter.");
					push("501 Syntax error in parameter.");
				}
			}

		} else if (command.equalsIgnoreCase("PASS")) {
			//this checks to see if the command has an extra space amended to it
			if (inputline.length() == 4) {
				System.out.println("500 Syntax error, command unrecognized.");
				push("500 Syntax error, command unrecognized.");
			} else {
				//this check is to make sure that there is a second token 
				//the second token is used as the parameter, in this case the password itself
				if (tokens.size() > 1) {
					parameter = tokens.get(1);
					if (parameter.matches("\\A\\p{ASCII}*\\z")) {
						System.out
								.println("230 Guest login OK.");
						push("230 Guest login OK.");
					} else {
						System.out.println("501 Syntax error in parameter.");
						push("501 Syntax error in parameter.");
					}
				} else {
					System.out.println("501 Syntax error in parameter.");
					push("501 Syntax error in parameter.");
				}

			}

		} else if (command.equalsIgnoreCase("USER")) {
			//this checks to see if the command has an extra space amended to it
			if (inputline.length() == 4) {
				System.out.println("500 Syntax error, command unrecognized.");
				push("500 Syntax error, command unrecognized.");
			} else {
				//this check is to make sure that there is a second token 
				//the second token is used as the parameter, in this case the username itself
				if (tokens.size() > 1) {
					parameter = tokens.get(1);
					if (parameter.matches("\\A\\p{ASCII}*\\z")) {
						System.out
								.println("331 Guest access OK, send password.");
						push("331 Guest access OK, send password.");
					} else {
						System.out.println("501 Syntax error in parameter.");
						push("501 Syntax error in parameter.");
					}
				} else {
					System.out.println("501 Syntax error in parameter.");
					push("501 Syntax error in parameter.");
				}

			}

		} else if (command.equalsIgnoreCase("PORT")) {
			//this checks to see if the command has an extra space amended to it
			if (inputline.length() == 4) {
				System.out.println("500 Syntax error, command unrecognized.");
				push("500 Syntax error, command unrecognized.");
			} else {
				// checks to make sure it's only the command and the parameter
				if (tokens.size() == 2) {
					parameter = tokens.get(1);
					if (parameter.matches("\\A\\p{ASCII}*\\z")) {
						String[] numbers = parameter.split(",");
						if (numbers.length == 6) {
							int correct = 0;
							//this verifies that all the numbers given in the port command are integers less than 255 
							// serving as a check for a valid IP address
							for(int i = 0; i < 6; i++){
								if(Integer.parseInt(numbers[i]) < 256){
									correct++;
								}
							}
							if(correct == 6){
							IP = numbers[0] + "." + numbers[1] + "."
									+ numbers[2] + "." + numbers[3];
							
							//this equation computes the port number supplied from the port parameter
							portNumber = (Integer.parseInt(numbers[4]) * 256)
									+ Integer.parseInt(numbers[5]);

							System.out.println("200 Port command successful ("
									+ IP + "," + portNumber + ").");
							push("200 Port command successful ("
									+ IP + "," + portNumber + ").");
							
							}else{
								System.out.println("501 Syntax error in parameter.");
								push("501 Syntax error in parameter.");
								
							}
						} else {
							System.out.println("501 Syntax error in parameter.");
							push("501 Syntax error in parameter.");
							
						}
					} else {
						System.out.println("501 Syntax error in parameter.");
						push("501 Syntax error in parameter.");
						
					}
				} else {
					System.out.println("501 Syntax error in parameter.");
					push("501 Syntax error in parameter.");
					
				}
			}
		} else if (command.equalsIgnoreCase("RETR")) {
			//this checks to see if the command has an extra space amended to it
			if (inputline.length() == 4) {
				System.out.println("500 Syntax error, command unrecognized.");
				push("500 Syntax error, command unrecognized.");
			} else {
				//check to make sure there's a filename 
				if (tokens.size() > 1) {
					parameter = tokens.get(1);
					if (parameter.matches("\\A\\p{ASCII}*\\z")) {
						
						//this takes care of any given slashes in front of the files destination 
						if(parameter.charAt(0) == '/' || parameter.charAt(0) == '\\' ){
							parameter = parameter.substring(1);
						}
						FileInputStream fis = null;
						
						//block to go try copying the file over
						try {
							
							try{ transfer = new Socket(IP, portNumber);
							}catch(Exception e){
								push("425 Can not open data connection.");
							}
							
							File file = new File(parameter);
							byte[] mybytearray = new byte[(int) file.length()];
							
							fis = new FileInputStream(file);
							BufferedInputStream bis = new BufferedInputStream(fis);
							
							System.out.println("150 File status okay.");
							push("150 File status okay.");

							DataInputStream dis = new DataInputStream(bis);
							dis.readFully(mybytearray, 0, mybytearray.length);
							
							OutputStream os = transfer.getOutputStream();
							
							//sending file to client
							DataOutputStream dos = new DataOutputStream(os);
							dos.writeLong(mybytearray.length);
							dos.write(mybytearray,0,mybytearray.length);
							dos.flush();
							
							fis.close();
							
							portNum = false;
							
							System.out
									.println("250 Requested file action completed.");
							push("250 Requested file action completed.");
						} catch (IOException e) {
							
							System.out
									.println("550 File not found or access denied.");
							push("550 File not found or access denied.");
						}
					} else {
						System.out.println("501 Syntax error in parameter.");
						push("501 Syntax error in parameter.");
					}
				} else {
					System.out.println("501 Syntax error in parameter.");
					push("501 Syntax error in parameter.");
				}

			}

		}else if(command.length() == 3 || command.length() == 4){
			System.out.println("502 Command not implemented.");
			push("502 Command not implemented.");
		} else {
			System.out.println("500 Syntax error, command unrecognized.");
			push("500 Syntax error, command unrecognized.");
		}
		}
		}
		
		// This method checks to see if a command is valid
		// beyond that it also handles the quit method which will terminate the program
		// also it handles certain error codes that cannot be handled within the parse method

	public static boolean isValid(String s) throws Exception {
		boolean valid = true;

		if (s.equalsIgnoreCase("user")) {
			user = true;
			userCount++;
		}

		if (s.equalsIgnoreCase("pass")) {
			pass = true;
			passCount++;
		}

		if (s.equalsIgnoreCase("port")) {
			portNum = true;
		}

		if (s.equalsIgnoreCase("quit")) {
			quit = true;
			return true;
		}
		
		

		if(user && pass){
			valid = true;
		}else if (user == false) {
			System.out.println("530 Not logged in.");
			push("530 Not logged in.");
			valid = false;
		}else if(user == false && pass == false){
			System.out.println("530 Not logged in.");
			push("530 Not logged in.");
			valid = false;
		}else if(user && !s.equalsIgnoreCase("pass") && !s.equalsIgnoreCase("user")){
			System.out.println("503 Bad sequence of commands.");
			push("503 Bad sequence of commands.");
			valid = false;
		}else if(user && s.equalsIgnoreCase("user") && userCount > 1){
			System.out.println("503 Bad sequence of commands.");
			push("503 Bad sequence of commands.");
			valid = false;
		}else if(pass && s.equalsIgnoreCase("pass") && passCount > 1){
			System.out.println("503 Bad sequence of commands.");
			push("503 Bad sequence of commands.");
			valid = false;
		}
		
		if (s.equalsIgnoreCase("retr") && portNum == false) {
			System.out.println("503 Bad sequence of commands.");
			push("503 Bad sequence of commands.");
			valid = false;
		}



		return valid;

	}

	static void push(String s) throws Exception{
		outToClient.writeBytes(s + CRLF);
		outToClient.flush();
	}
	

}//end class
