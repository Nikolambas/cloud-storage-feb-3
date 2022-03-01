package com.geekbrains.cloud.nio;


import jdk.internal.util.xml.impl.Input;

import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;

public class EchoServerNio {
        private ServerSocketChannel serverSocketChannel;
        private Selector selector;
        private ByteBuffer buf;
        private File file;
        private Path path;

        public EchoServerNio() throws IOException {
            buf=ByteBuffer.allocate(5);
            serverSocketChannel=ServerSocketChannel.open();
            selector=Selector.open();

            serverSocketChannel.bind(new InetSocketAddress(8189));
            serverSocketChannel.configureBlocking(false);

            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (serverSocketChannel.isOpen()){
                selector.select();
                Set<SelectionKey>keys = selector.selectedKeys();

                Iterator<SelectionKey>iterator = keys.iterator();
                while (iterator.hasNext()){
                    SelectionKey currentKey = iterator.next();
                    if (currentKey.isAcceptable()){
                        handleAccept();
                    }
                    if (currentKey.isReadable()){
                        handleRead(currentKey);
                    }
                    iterator.remove();
                }
            }
        }

    private void handleRead(SelectionKey currentKey) throws IOException {
        SocketChannel channel = (SocketChannel) currentKey.channel();

        StringBuilder reader = new StringBuilder();

        while (true) {

            int count = channel.read(buf);

            if (count == 0) {
                break;
            }

            if (count == -1) {
                channel.close();
                return;
            }
            buf.flip();

            while (buf.hasRemaining()) {
                reader.append((char) buf.get());
            }

            buf.clear();
        }
        String[]messageArr=reader.toString().split(" ");

        if (messageArr.length==1&&messageArr[0].startsWith("ls")){
            getFileDir(channel);
        }
        else if (reader.toString().startsWith("--help")){
            Path help = Paths.get("src","main","java","com","geekbrains","cloud","nio","help");
            byte [] buf=Files.readAllBytes(help);
            channel.write(ByteBuffer.wrap(buf));
        }
        else if (messageArr.length==2&&messageArr[0].equals("cd")){
            path=path.resolve(messageArr[1].trim());
            path.normalize();

        }
        else if (messageArr.length==2&&messageArr[0].equals("cat")){
            try {
                Path path1=path.resolve(messageArr[1].trim());
                byte [] buf=Files.readAllBytes(path1);
                channel.write(ByteBuffer.wrap(buf));
            }catch (FileNotFoundException e){
                channel.write(ByteBuffer.wrap("Файл не существует".getBytes(StandardCharsets.UTF_8)));
            }
        }
        else if (messageArr.length==2&&messageArr[0].equals("mkdir")) {
            try {
                path=path.resolve(messageArr[1].trim());
                Files.createDirectory(path);
            } catch (FileAlreadyExistsException e) {
                channel.write(ByteBuffer.wrap("Папка уже создана".getBytes(StandardCharsets.UTF_8)));
            }
        }
        else if (messageArr.length == 2 && messageArr[0].equals("touch")) {
            try{
                Path pathFile=path.resolve(messageArr[1].trim());
                Files.createFile(pathFile);
            }catch (FileAlreadyExistsException e ){
                channel.write(ByteBuffer.wrap("Фаил уже создан".getBytes(StandardCharsets.UTF_8)));
            }
        } else {
            String msg = "--> " + reader.toString();
            System.out.println("Received: " + msg);
            channel.write(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
            }
        }

    private void getFileDir(SocketChannel channel) throws IOException {
        try {
            file = path.toFile();
            String[] files = file.list();
            for (int i = 0; i < files.length; i++) {
                channel.write(ByteBuffer.wrap(files[i].getBytes(StandardCharsets.UTF_8)));
                channel.write(ByteBuffer.wrap("\n".getBytes(StandardCharsets.UTF_8)));
            }
        }catch (NullPointerException e){
            channel.write(ByteBuffer.wrap("Папка пуста".getBytes(StandardCharsets.UTF_8)));
        }
    }


    private void handleAccept() throws IOException {
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        path= Paths.get("server");
        socketChannel.write(ByteBuffer.wrap("Welcome to Nik terminal"
                .getBytes(StandardCharsets.UTF_8)));
        System.out.println("Client accepted...");
    }

    public static void main(String[] args) throws IOException {
            new EchoServerNio();
    }
}
