package order;

import java.util.List;
import org.junit.Test;
import org.junit.Before;
import io.qameta.allure.Epic;
import io.qameta.allure.junit4.DisplayName;
import entity.User;
import entity.Order;
import api.UserClient;
import api.OrderClient;
import utils.GenerateUser;
import org.apache.commons.lang3.StringUtils;
import io.restassured.response.ValidatableResponse;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Epic("Create order")
public class OrderCreateTest {
    private static final String MESSAGE_BAD_REQUEST = "Ingredient ids must be provided";
    private ValidatableResponse response;
    private User user;
    private Order order;
    private UserClient userClient;
    private OrderClient orderClient;

    @Before
    public void setUp() {
        user = GenerateUser.getRandomUser();
        order = new Order();
        userClient = new UserClient();
        orderClient = new OrderClient();
    }

    @Test
    @DisplayName("Create order by authorization")
    public void orderCreateByAuthorization() {
        fillListIngredients();
        response = userClient.createUser(user);
        String accessToken = response.extract().path("accessToken");
        response = userClient.loginUser(user, accessToken);
        response = orderClient.createOrderByAuthorization(order, accessToken);
        int statusCode = response.extract().statusCode();
        boolean isCreate = response.extract().path("success");
        int orderNumber = response.extract().path("order.number");
        String orderId = response.extract().path("order._id");
        response = userClient.deleteUser(StringUtils.substringAfter(accessToken, " "));

        assertThat("Code not equal", statusCode, equalTo(SC_OK));
        assertThat("Order is create incorrect", isCreate, equalTo(true));
        assertThat("Order number is null", orderNumber, notNullValue());
        assertThat("Order id is null", orderId, notNullValue());
    }

    @Test
    @DisplayName("Create order without authorization")
    public void orderCreateWithoutAuthorization() {
        fillListIngredients();
        response = orderClient.createOrderWithoutAuthorization(order);
        int statusCode = response.extract().statusCode();
        boolean isCreate = response.extract().path("success");
        int orderNumber = response.extract().path("order.number");

        assertThat("Code not equal", statusCode, equalTo(SC_OK));
        assertThat("Order is create incorrect", isCreate, equalTo(true));
        assertThat("Order number is null", orderNumber, notNullValue());
    }

    @Test
    @DisplayName("Create order without authorization and ingredients")
    public void orderCreateWithoutAuthorizationAndIngredients() {
        response = orderClient.createOrderWithoutAuthorization(order);
        int statusCode = response.extract().statusCode();
        String message = response.extract().path("message");
        boolean isCreate = response.extract().path("success");

        assertThat("Code not equal", statusCode, equalTo(SC_BAD_REQUEST));
        assertThat("Message not equal", message, equalTo(MESSAGE_BAD_REQUEST));
        assertThat("Order is create correct", isCreate, equalTo(false));
    }

    @Test
    @DisplayName("Create order without authorization and change hash ingredient")
    public void orderCreateWithoutAuthorizationAndChangeHashIngredient() {
        response = orderClient.getAllIngredients();
        List<String> list = response.extract().path("data._id");
        List<String> ingredients = order.getIngredients();
        ingredients.add(list.get(0));
        ingredients.add(list.get(5).replaceAll("a", "l"));
        ingredients.add(list.get(0));
        response = orderClient.createOrderWithoutAuthorization(order);
        int statusCode = response.extract().statusCode();

        assertThat("Code not equal", statusCode, equalTo(SC_INTERNAL_SERVER_ERROR));

    }

    private void fillListIngredients() {
        response = orderClient.getAllIngredients();
        List<String> list = response.extract().path("data._id");
        List<String> ingredients = order.getIngredients();
        ingredients.add(list.get(0));
        ingredients.add(list.get(5));
        ingredients.add(list.get(0));
    }
}