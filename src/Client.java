import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.Paths;
import java.util.*;

public class Client {

    private static final String SERVER = "localhost"; // Địa chỉ POP3 Server
    private static final int PORT_POP3 = 110;              // Cổng POP3
    private static final int PORT_SMTP = 25;
    private Socket socket;
    private BufferedReader serverInput;
    private PrintWriter serverOutput;
    private String username;
    private String password;
    private boolean loggedIn=false;
    public static final int n=1000000;
    public static final Object[][] userLoginStatus = new Object[n][2];
    private int z=0;
    private Path outputPath;
    String[] filePathes;
    private int countMails;
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }

    // Hàm chính để hiển thị menu và xử lý các lựa chọn
    public void start() {
        try {
            for (int i =0;i<n;i++) {
                userLoginStatus[i][0]="";
                userLoginStatus[i][1]=false;
            }
            while (true) {
                // Hiển thị menu
                System.out.println("\n--- MENU ---");
                System.out.println("1. Đăng nhập");
                System.out.println("2. Xem danh sách email");
                System.out.println("3. Xem nội dung email");
                System.out.println("4. Gửi email");
                System.out.println("5. Thoát");
                System.out.print("Chọn một chức năng: ");

                String choice = scanner.next();

                scanner.nextLine(); // Đọc bỏ dòng thừa

                switch (choice) {
                    case "1":
                        loggedIn=true;
                        login(scanner); // Đăng nhập
                        break;
                    case "2":
                        if (!loggedIn) {
                            System.out.println("Hãy đăng nhập trước!");
                            break;
                        }
                        listEmails(); // Xem danh sách email
                        break;
                    case "3":
                        if (!loggedIn) {
                            System.out.println("Hãy đăng nhập trước!");
                            break;
                        }
                        readEmail(scanner); // Xem nội dung email
                        break;
                    case "4":
                        if (!loggedIn) {
                            System.out.println("Hãy đăng nhập trước!");
                            break;
                        }
                        sendEmail(scanner); // Gửi email
                        break;
                    case "5":
                        if (loggedIn) {
                            loggedIn=false;
                            disconnect();
                        }
                        System.out.println("Đã thoát chương trình.");
                        return;
                    default:
                        System.out.println("Lựa chọn không hợp lệ!");
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi: " + e.getMessage());
        }
    }

    // Kết nối tới server
    private void connectToServer(int PORT) throws IOException {
        socket = new Socket(SERVER, PORT);
        serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        serverOutput = new PrintWriter(socket.getOutputStream(), true);

        // Đọc phản hồi ban đầu từ server
        String response = serverInput.readLine();
        //System.out.println("Server Response: " + response);
        if (response != null) {
            //System.out.println("Server: " + response);
        } else {
            System.out.println("Server không gửi phản hồi ban đầu.");
        }
    }

    // Đăng nhập
    private void login(Scanner scanner) throws IOException {
        connectToServer(PORT_POP3);
        System.out.print("Nhập username: ");
        String username = scanner.nextLine();
        serverOutput.println("USER " + username);
        serverInput.readLine();
        //System.out.println("Server: " + serverInput.readLine());
        System.out.print("Nhập password: ");
        String password = scanner.nextLine();
        serverOutput.println("PASS " + password);
        String response = serverInput.readLine();
        userLoginStatus[z][0]=username;
        //System.out.println("Server: " + response);
        if (response.startsWith("+OK") || response.startsWith("2")) {
            boolean checkLogin=false;
            for (int k =0;k<z;k++) {
                if (userLoginStatus[k][0].equals(username) && userLoginStatus[k][1].equals(true)) {
                    checkLogin=true;
                    break;
                }
            }
            if (!checkLogin) {
                this.username = username;
                this.password = password;
                System.out.println("Đăng nhập thành công!");
                userLoginStatus[z][1]=true;
                for (int k=0;k<z;k++) {
                    userLoginStatus[k][1]=false;
                }
            } else {
                System.out.println("Tài khoản đang đăng nhập. Vui lòng đăng nhập bằng tài khoản khác !");
            }
            z++;
        } else {
            System.out.println("Đăng nhập thất bại. Vui lòng thử lại.");
        }
        serverOutput.println("QUIT");
    }
    private void reConnectToPOP3() throws IOException {
        // Thiết lập kết nối với POP3 server
        connectToServer(PORT_POP3);
        serverOutput.println("USER " + username);
        serverInput.readLine();
        //System.out.println("Server: " + serverInput.readLine());
        serverOutput.println("PASS " + password);
        serverInput.readLine();
        //System.out.println("Server: " + serverInput.readLine());
    }

    // Lấy danh sách email
    private void listEmails() throws IOException {

        reConnectToPOP3();
        serverOutput.println("LIST");
        String response = serverInput.readLine();
        response = response.replaceAll("[()]","");
        String[] res = response.split(" ");
        System.out.println("Gồm "+res[1]+" mails. Tổng kích thước: "+res[3]+" octets");

        if (response.startsWith("+OK") || response.startsWith("2")) {
            String line;
            while (!(line = serverInput.readLine()).equals(".")) {
                String[] task = line.split(" ");
                System.out.println("ID:"+task[0]+" - Kích thước mail: "+task[1]+" octets"); // In từng email (ID và kích thước)
            }
        } else {
            System.out.println("Không thể lấy danh sách email.");
        }
        serverOutput.println("QUIT");
        String quitResponse = serverInput.readLine();
        if (!quitResponse.startsWith("+OK")) {
            throw new IOException("QUIT command failed: " + quitResponse);
        }
    }

    // Xem nội dung email
    private void readEmail(Scanner scanner) throws IOException {

        reConnectToPOP3();

        StringBuilder emailContent = new StringBuilder();

        System.out.print("Nhập ID email muốn đọc: ");
        int emailId = scanner.nextInt();
        scanner.nextLine(); // Đọc bỏ dòng thừa

        serverOutput.println("RETR " + emailId);
        String response = serverInput.readLine();

        if (response.startsWith("+OK") || response.startsWith("2")) {

            String line;
            while ((line = serverInput.readLine()) != null) {
                if (line.equals(".")) { // Dấu hiệu kết thúc
                    break;
                }
                emailContent.append(line).append("\n");
            }
        } else {
            System.out.println("Không thể đọc email.");
        }
        if (emailContent.toString().isEmpty()) {
            System.out.println("Mail ID không tồn tại !");
            serverOutput.println("QUIT");
            String quitResponse = serverInput.readLine();
            if (!(quitResponse.startsWith("+OK") || quitResponse.startsWith("2"))) {
                throw new IOException("QUIT command failed: " + quitResponse);
            }
            return;
        }
        //System.out.println(emailContent);

        String sendMail = null;
        String receivedMail = null;
        String subject = null;
        String time = null;
        String content = null;
        String[] parts;
        String[] header;
        boolean attachedFile;


        if (emailContent.toString().contains("--boundary123")) {
            parts = emailContent.toString().split("\n--boundary123\n", 0);
            header = parts[0].split("\n", 0);
            countMails = emailContent.toString().split("Content-Disposition: attachment;").length-1;
            attachedFile = true;
        } else {
            parts = emailContent.toString().split("\n", 0);
            attachedFile = false;
            header = parts;
        }
        int countLine = 0;
        // Lấy thông tin từ header email
        for (String line : header) {
            countLine++;
            line = line.stripLeading();
            if (line.startsWith("From: ")) {
                sendMail = line.split(": ", 2)[1];
            } else if (line.startsWith("To: ")) {
                receivedMail = line.split(": ", 2)[1];
            } else if (line.startsWith("Subject: ")) {
                subject = line.split(": ", 2)[1];
            } else if (line.startsWith("; ")) {
                time = line.split("; ", 2)[1];
            } else if (line.startsWith("Content-Type: ")) {
                content = "";
                for (int i = countLine+1; i < header.length; i++) {
                    if (i != header.length - 1) {
                        content += header[i] + "\n";
                    } else {
                        content += header[i];
                    }
                }
                break;
            }
        }

        String[] contentParts;
        if (attachedFile) {
            contentParts = parts[1].split("\n", 3);
            content = contentParts[2];
        }

        // In ra thông tin email
        System.out.println("---------- Thông tin Email ----------");
        System.out.println("Người gửi: " + sendMail);
        System.out.println("Người nhận: " + receivedMail);
        System.out.println("Chủ đề: " + subject);
        System.out.println("Thời gian gửi: " + time);
        System.out.println("Nội dung:\n" + content);

        // Tải file đính kèm nếu có
        if (attachedFile) {
            System.out.print("Mail này chứa "+countMails +" File đính kèm. Bạn muốn download file đính kèm từ mail không? (1: Có, 0: Không): ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Đọc bỏ dòng thừa
            if (choice == 1) {
                for (String part : parts) {
                    if (part.contains("Content-Disposition: attachment") && part.contains("Content-Transfer-Encoding: base64")) {
                        // Lấy tên file từ Content-Disposition
                        String fileName = null;
                        if (part.contains("filename=\"")) {
                            fileName = part.split("filename=\"")[1].split("\"")[0];
                        } else {
                            System.out.println("Không tìm thấy tên file trong phần đính kèm.");
                            continue;
                        }

                        // Lấy nội dung Base64
                        String base64Content;
                        String[] mimeParts = part.split("filename=\"", 2)[1].split("\"")[1].split("--boundary123",2);
                        //System.out.println("1233333333333"+ Arrays.toString(mimeParts));
                        if (mimeParts.length == 2) {
                            base64Content = mimeParts[0].replaceAll("\n","").trim();
                            //System.out.println(base64Content);
                        } else {
                            System.out.println("Không tìm thấy nội dung mã hóa.");
                            continue;
                        }
                        Path directory;
                        do {
                            // Yêu cầu người dùng nhập thư mục lưu file
                            System.out.print("Nhập thư mục lưu trữ: ");
                            String directoryPath = scanner.nextLine();
                            directory = Paths.get(directoryPath);
                            if (Files.exists(directory) && Files.isDirectory(directory) && Files.isWritable(directory)) {
                                break;
                            }
                            else {
                                System.out.println("Dường dẫn đến file "+fileName+" không hợp lệ ! Yêu cầu nhập lại.");
                            }
                        }
                        while (true);

                        // Xác định đường dẫn đầy đủ cho file
                        outputPath = directory.resolve(fileName);

                        // Giải mã và lưu file
                        try {
                            byte[] fileBytes = Base64.getDecoder().decode(base64Content);
                            java.nio.file.Files.write(outputPath, fileBytes);

                        } catch (IllegalArgumentException e) {
                            System.out.println("Dữ liệu Base64 không hợp lệ: " + e.getMessage());
                        } catch (IOException e) {
                        System.out.println("Lỗi khi lưu file: " + e.getMessage());
                        }
                    }
                }
                serverOutput.println("DELE "+emailId);
                serverInput.readLine();
                System.out.println("Đã tải file đính kèm vào " + outputPath + " và xóa mail có ID: " +emailId + " khỏi hộp thư.");
            }
        }

        // Thực hiện QUIT command để kết thúc phiên làm việc với POP3 server
        serverOutput.println("QUIT");
        String quitResponse = serverInput.readLine();
        if (!(quitResponse.startsWith("+OK") || quitResponse.startsWith("2"))) {
            throw new IOException("QUIT command failed: " + quitResponse);
        }
    }


    private void inputEmailContent(StringBuilder emailContent) {
        String line;
        serverOutput.println("Content-Type: text/plain; charset=UTF-8");
        serverOutput.println();
        System.out.println("Nhập nội dung email (gõ 'END' khi kết thúc): ");
        while (!(line = scanner.nextLine()).equals("END")) {
            emailContent.append(line).append("\n");
        }
        serverOutput.println(emailContent); // Gửi nội dung văn bản
    }
    private void sendEmail(Scanner scanner) throws IOException {

        connectToServer(PORT_SMTP);

        StringBuilder emailContent = new StringBuilder();
        String line;

        // Gửi EHLO để kiểm tra kết nối
        serverOutput.println("EHLO localhost");
        String response = serverInput.readLine();
        if (response.startsWith("-ERR")) {
            System.out.println("Không thể kết nối đến SMTP Server");
            return;
        }

        String senderEmail = username;
        serverOutput.println("MAIL FROM:" + senderEmail);
        serverInput.readLine();
        //System.out.println("Server: " + serverInput.readLine());

        System.out.print("Nhập email nhận thư: ");
        String recipientEmail = scanner.nextLine();
        serverOutput.println("RCPT TO:" + recipientEmail);
        serverInput.readLine();
        //System.out.println("Server: " + serverInput.readLine());

        serverOutput.println("DATA");
        serverInput.readLine();
        //System.out.println("Server: " + serverInput.readLine());

        serverOutput.println("From: " + senderEmail);
        serverOutput.println("To: " + recipientEmail);

        System.out.print("Subject: ");
        String subject = scanner.nextLine();
        serverOutput.println("Subject: " + subject);

        System.out.print("Nhập đường dẫn file đính kèm (hoặc để trống nếu không có): ");
        String filePaths = scanner.nextLine();

        if (!filePaths.isEmpty()) {
            filePaths = filePaths.trim();
            filePathes = filePaths.split(" ",0);
            while (true) {
                boolean checkFilePath = true;
                for (String filePath: filePathes) {
                    Path file = Paths.get(filePath);
                    if (!java.nio.file.Files.exists(file) || !java.nio.file.Files.isReadable(file)) {
                        checkFilePath = false;
                        break;
                    }
                }
                if (checkFilePath) {
                    break;
                }
                System.out.print("File không tồn tại. Hãy nhập lại đường dẫn file đính kèm: ");
                filePaths = scanner.nextLine();
            }
            // Header MIME multipart
            serverOutput.println("MIME-Version: 1.0");
            serverOutput.println("Content-Type: multipart/mixed; boundary=\"boundary123\"");
            serverOutput.println(); // Dòng trống phân cách giữa header và body

            // Body email (text)
            serverOutput.println("--boundary123");
            inputEmailContent(emailContent);

            // Kiểm tra và đính kèm file
            for (String filepath : filePathes) {
                attachFile(filepath);
            }
            serverOutput.println("--boundary123--"); // Kết thúc phần MIME của email
        } else {
            // Header MIME đơn giản khi không có file đính kèm
            serverOutput.println("MIME-Version: 1.0");
            inputEmailContent(emailContent);
        }

        // Kết thúc email
        serverOutput.println(".");
        String responsefinal = serverInput.readLine();
        if (responsefinal.startsWith("+OK") || responsefinal.startsWith("2")) {
            System.out.println("Bạn đã gửi thư thành công !");
        }
        else {
            System.out.println("Lỗi gửi thư !");
        }
        serverOutput.println("QUIT");
        String quitResponse = serverInput.readLine();
        if (!(quitResponse.startsWith("+OK") || quitResponse.startsWith("2"))) {
            throw new IOException("QUIT command failed: " + quitResponse);
        }
    }


    // Hàm đính kèm file và mã hóa Base64
    private void attachFile(String filePath) throws IOException {
        Path file = Paths.get(filePath);
        String fileName = file.getFileName().toString();
        String mimeType = getMimeType(filePath);
        String encodedFile = encodeFileToBase64(filePath);
        //System.out.println(encodedFile);
        // Gửi phần đính kèm
        serverOutput.println("--boundary123");
        serverOutput.println("Content-Type: " + mimeType + "; name=\"" + fileName + "\"");
        serverOutput.println("Content-Transfer-Encoding: base64");
        serverOutput.println("Content-Disposition: attachment; filename=\"" + fileName + "\"");
        serverOutput.println();
        serverOutput.println(encodedFile); // Gửi dữ liệu Base64 của file

    }

    // Hàm mã hóa file sang Base64
    private String encodeFileToBase64(String filePath) throws IOException {
        byte[] fileContent = java.nio.file.Files.readAllBytes(Paths.get(filePath));
        String base64 = Base64.getEncoder().encodeToString(fileContent);
        StringBuilder sb = new StringBuilder();
        int lineLength = 76; // SMTP khuyến nghị mỗi dòng không quá 76 ký tự
        for (int i = 0; i < base64.length(); i += lineLength) {
            sb.append(base64, i, Math.min(i + lineLength, base64.length())).append("\n");
        }
        return sb.toString();
    }

    private String getMimeType(String filePath) {
            // Xác định MIME type dựa trên phần mở rộng của tệp
        String mimeType;
        String fileName = filePath.substring(filePath.lastIndexOf("\\")+1);
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                mimeType = "image/jpeg";
                break;
            case "png":
                mimeType = "image/png";
                break;
            case "gif":
                mimeType = "image/gif";
                break;
            case "mp3":
                mimeType = "audio/mpeg";
                break;
            case "mp4":
                mimeType = "video/mp4";
                break;
            case "txt":
                mimeType = "text/plain";
                break;
            case "pdf":
                mimeType = "application/pdf";
                break;
            default:
                mimeType = "application/octet-stream";  // Loại tệp mặc định
                break;
        }

        return mimeType;
    }

    // Ngắt kết nối
    private void disconnect() throws IOException {
        this.username = null;
        this.password = null;
        serverOutput.println("QUIT");
        serverInput.readLine();
        //System.out.println("Server: " + serverInput.readLine());
        socket.close();
    }
}
