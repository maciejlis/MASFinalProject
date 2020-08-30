package controller;

import dao.BikeDao;
import dao.BikeOrderDao;
import dao.ClientDao;
import dao.SellerDao;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.bikeservice.Bike;
import model.bikeservice.BikeOrder;
import model.bikeservice.enums.OrderState;
import model.user.Client;
import model.user.Seller;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SummaryController implements Initializable {

    @FXML
    private TextField name;

    @FXML
    private TextField surname;

    @FXML
    private TextField phone;

    @FXML
    private TextField street;

    @FXML
    private TextField city;

    @FXML
    private TextField postCode;

    @FXML
    private Label bikeLabel;

    @FXML
    private Label acceptLabel;

    @FXML
    private Label insertLabel;

    @FXML
    private GridPane inputGrid;

    @FXML
    private BorderPane pickedModel;

    @FXML
    private Button acceptButton;

    @FXML
    private HBox summaryButtons;

    private List<Bike> bikeList;

    private double summaryPrice;

    private Stage orderStage;

    private Stage summaryStage;

    private OrderController orderController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        pickedModel.setVisible(false);
        summaryButtons.setVisible(false);
        acceptLabel.setVisible(false);

    }

    @FXML
    public void buttonSummarize() {
        if (!ifAnyInputTextFieldEmpty()) {

            inputGrid.setDisable(true);

            pickedModel.setVisible(true);
            summaryButtons.setVisible(true);
            acceptLabel.setVisible(true);
            insertLabel.setVisible(false);
            acceptButton.setVisible(false);

            Client client = ClientDao.getInstance().getByLogin(name.getText());

            bikeList.forEach(bike -> {
                setSummaryPrice(summaryPrice + bike.getCost());
            });

            if(client != null){
                summaryPrice -= (((double)client.getDiscount()/100) * summaryPrice);
            }

            bikeLabel.setText(summaryPrice + " PLN");
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Wprowadź poprawnie wszystkie pola", ButtonType.OK);
            alert.showAndWait();
        }
    }

    @FXML
    public void editButtonAction() {
        inputGrid.setDisable(false);

        pickedModel.setVisible(false);
        summaryButtons.setVisible(false);
        acceptLabel.setVisible(false);
        insertLabel.setVisible(true);
        acceptButton.setVisible(true);
    }

    @FXML
    public void acceptOrder() {
        summarizeOrderAndSave();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Zamówienie zostało złożone poprawnie", ButtonType.OK);
        alert.showAndWait();

        //reload the bikes
        orderController.getTableView().setItems(orderController.loadBikes());

        //deselect all checkboxes
        orderController.getOrderedBikes().values().forEach(c -> c.setSelected(false));

        orderStage.show();
        summaryStage.close();
    }

    @FXML
    public void cancelOrder() {
        Alert alert = new Alert(Alert.AlertType.WARNING, "Czy napewno chcesz anulować ?", ButtonType.CANCEL, ButtonType.YES);
        Optional<ButtonType> resultbutton = alert.showAndWait();

        if (resultbutton.get().equals(ButtonType.YES)) {
            orderStage.show();
            summaryStage.close();
        }
    }

    private boolean ifAnyInputTextFieldEmpty() {
        ObservableList<Node> children = inputGrid.getChildren();

        for (Node node : children) {
            if (GridPane.getColumnIndex(node) != null && GridPane.getColumnIndex(node) == 1) {
                TextField textField = (TextField)node;
                if(textField.getText().equals("")){
                    return true;
                }
            }
        }
        return false;
    }

    private void summarizeOrderAndSave() {
        String address = buildAddress(phone.getText(), street.getText(), city.getText(), postCode.getText());

        Seller seller = new Seller("Maciek", "Lis", "Koszykowa 86, 01-400 Warszawa", "admin" , "admin", LocalDate.now(), 3000);
        Client client = new Client(name.getText(), surname.getText(), address, name.getText(), "maciek");

        Client oldClient = ClientDao.getInstance().getByLogin(client.getLogin());
        Seller oldSeller = SellerDao.getInstance().getByLogin(seller.getLogin());

        if(oldClient == null){
            ClientDao.getInstance().save(client);
        }else{
            client = oldClient;
        }

        if(oldSeller == null){
            SellerDao.getInstance().save(seller);
        }else{
            seller = oldSeller;
        }

        BikeOrder bikeOrder = new BikeOrder(OrderState.ORDERED, summaryPrice, java.time.LocalDate.now(), address, client.getDiscount());

        for(Bike bike: bikeList) {
            bikeOrder.addBike(bike);
        }

        client.addOrder(bikeOrder);
        seller.addOrder(bikeOrder);

        BikeOrderDao.getInstance().save(bikeOrder);

        ClientDao.getInstance().update(client);
        SellerDao.getInstance().update(seller);

        BikeDao.getInstance().updateAll(bikeList);
    }

    private void setSummaryPrice(double summaryPrice) {
        this.summaryPrice = summaryPrice;
    }

    void setOrderStage(Stage stage) {
        this.orderStage = stage;
    }

    void setStage(Stage stage) {
        this.summaryStage = stage;
    }

    void setOrderController(OrderController orderController){
        this.orderController = orderController;
    }

    void transferData(List<Bike> bikeList) {
        this.bikeList = bikeList;
    }

    private String buildAddress(String phone, String street, String city, String postCode) {
        return phone +
                "," +
                street +
                "," +
                city +
                "," +
                postCode;
    }
}
