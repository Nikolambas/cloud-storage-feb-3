package com.geekbrains.cloud.client;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class MainController implements Initializable {

    private static final int BUFFER_SIZE = 8192;

    public TextField clientPath;
    public TextField serverPath;
    public ListView<String> clientView;
    public ListView<String> serverView;
    private File currentDirectory;
    private String serverDirectory;

    private DataInputStream is;
    private DataOutputStream os;
    private byte[] buf;

    private void updateClientView() {
        Platform.runLater(() -> {
            clientPath.setText(currentDirectory.getAbsolutePath());
            clientView.getItems().clear();
            clientView.getItems().add("...");
            clientView.getItems()
                    .addAll(currentDirectory.list());
        });
    }

    private void updateServerView()  {
        try {
            os.writeUTF("#Update_server_view#");
            os.writeUTF(serverDirectory);
            Platform.runLater(()-> {
                try {
                    serverPath.setText(is.readUTF());
                    serverView.getItems().clear();
                    serverView.getItems().add("...");
                    while (is.available() > 0) {
                        serverView.getItems().addAll(is.readUTF());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void download(ActionEvent actionEvent) {
        String item = serverView.getSelectionModel().getSelectedItem();
        try {
            os.writeUTF("#Server_to_Client#");
            os.writeUTF(item);
            String name = is.readUTF();
            long size = is.readLong();
            File newFile = currentDirectory.toPath()
                    .resolve(name)
                    .toFile();
            try (OutputStream fos = new FileOutputStream(newFile)) {
                for (int i = 0; i < (size + BUFFER_SIZE - 1) / BUFFER_SIZE; i++) {
                    int readCount = is.read(buf);
                    fos.write(buf, 0, readCount);
                }
            }
            updateClientView();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        String item = clientView.getSelectionModel().getSelectedItem();
        File selected = currentDirectory.toPath().resolve(item).toFile();
        if (selected.isFile()) {
            os.writeUTF("#Client_to_Server#");
            os.writeUTF(selected.getName());
            os.writeLong(selected.length());
            try (InputStream fis = new FileInputStream(selected)) {
                while (fis.available() > 0) {
                    int readBytes = fis.read(buf);
                    os.write(buf, 0, readBytes);
                }
            }
            os.flush();
            updateServerView();
        }
    }

    private void initNetwork() {
        try {
            serverDirectory = "...";
            buf = new byte[BUFFER_SIZE];
            Socket socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentDirectory = new File(System.getProperty("user.home"));
        updateClientView();
        initNetwork();
        updateServerView();
        clientView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = clientView.getSelectionModel().getSelectedItem();
                if (item.equals("...")) {
                    currentDirectory = currentDirectory.getParentFile();
                    updateClientView();
                } else {
                    File selected = currentDirectory.toPath().resolve(item).toFile();
                    if (selected.isDirectory()) {
                        currentDirectory = selected;
                        updateClientView();
                    }
                }
            }
        });
        serverView.setOnMouseClicked(event -> {
            if (event.getClickCount()==2){
                serverDirectory = serverView.getSelectionModel().getSelectedItem();
                updateServerView();
            }
        });
    }
}
