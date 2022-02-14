package com.geekbrains.cloud.server;


import java.io.*;
import java.net.Socket;

public class CloudFileHandler implements Runnable {

    private static final int BUFFER_SIZE = 8192;
    private final DataInputStream is;
    private final DataOutputStream os;
    private final byte[] buf;
    private File serverDirectory;


    public CloudFileHandler(Socket socket) throws IOException {
        System.out.println("Client connected!");
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        buf = new byte[BUFFER_SIZE];
        serverDirectory = new File("server");

    }

    private void outServerView(){
        try {
            String serDir = is.readUTF();
            if (serDir.equals("...")){
                serverDirectory=new File("server");
            }else  {
                File selected = serverDirectory.toPath().resolve(serDir).toFile();
                if (selected.isDirectory()) {
                    serverDirectory = selected;
                }
            }
            System.out.println(serverDirectory.getName());
            os.writeUTF(serverDirectory.getAbsolutePath());
            String [] list = serverDirectory.list();
            for (int i = 0; i < list.length; i++) {
                os.writeUTF(list[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadFile(){
        try {
            String name = is.readUTF();
            File selected = serverDirectory.toPath().resolve(name).toFile();
            if (selected.isFile()){
                os.writeUTF(selected.getName());
                os.writeLong(selected.length());
                try (InputStream fis = new FileInputStream(selected)) {
                    while (fis.available() > 0) {
                        int readBytes = fis.read(buf);
                        os.write(buf, 0, readBytes);
                    }
                }
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadFile(){
        try {
            String name = is.readUTF();
            long size = is.readLong();
            File newFile = serverDirectory.toPath()
                    .resolve(name)
                    .toFile();
            try (OutputStream fos = new FileOutputStream(newFile)) {
                for (int i = 0; i < (size + BUFFER_SIZE - 1) / BUFFER_SIZE; i++) {
                    int readCount = is.read(buf);
                    fos.write(buf, 0, readCount);
                }
            }
            System.out.println("File: " + name + " is uploaded");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        try {
            while (true) {
                String command = is.readUTF();
                if ("#Update_server_view#".equals(command)){
                    outServerView();
                }if ("#Client_to_Server#".equals(command)){
                    downloadFile();
                }if ("#Server_to_Client#".equals(command)){
                    uploadFile();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
