import Main.Java.Entities.Booking;
import Main.Java.Entities.BookingDates;
import Main.Java.Entities.User;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static org.hamcrest.Matchers.*;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BookingTests {
    public static Faker faker;
    private static RequestSpecification request;
    private static Booking booking;
    private static BookingDates bookingDates;
    private static User user;

    @BeforeAll
    public static void Setup(){
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";
        faker = new Faker();
        user = new User(faker.name().username(),
                faker.name().firstName(),
                faker.name().lastName(),
                faker.internet().safeEmailAddress(),
                faker.internet().password(8,10),
                faker.phoneNumber().toString());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        bookingDates = new BookingDates(sdf.format(faker.date().past(1, TimeUnit.DAYS)), sdf.format(faker.date().future(1, TimeUnit.DAYS)));
        booking = new Booking(user.getFirstName(), user.getLastName(),
                (float)faker.number().randomDouble(2, 50, 100000),
                true,bookingDates,
                "");
        RestAssured.filters(new RequestLoggingFilter(),new ResponseLoggingFilter(), new ErrorLoggingFilter());
    }

    @BeforeEach
    void setRequest(){
        request = given().config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                .contentType(ContentType.JSON)
                .auth().basic("admin", "password123")
        ;
    }

    @Test // Create booking
    public void  CreateBooking_WithValidData_returnOk(){
        request
            .contentType(ContentType.JSON)
            .when()
                .body(booking)
                .post("/booking")
            .then()
                .body(matchesJsonSchemaInClasspath("createBookingRequestSchema.json"))
            .and()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON).and().time(lessThan(2000L))
            .extract()
                .jsonPath()
        ;
    }

    @Test // create token
    public void CreateAuthToken(){
        Map<String, String> body = new HashMap<>();
        body.put("username", "admin");
        body.put("password", "password123");

        request
            .contentType(ContentType.JSON)
            .when()
                .body(body)
                .post("/auth")
            .then()
                .assertThat()
                .statusCode(200)
            .extract()
                .path("token")
        ; 
    }

    @Test // Get Booking id list
    public void getAllBookingIds_returnOk(){
        Response response = request
            .when()
                .get("/booking")
            .then()
                .extract()
                .response()
        ;

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.statusCode());
    }

    @Test // Get booking by id
    public void getBookingById_returnOk(){
        request
            .when()
                .get("/booking/" + 7771)
            .then()
                .assertThat()
                .statusCode(200)
        ;
    }

    @Test
    public void  getAllBookingsByUserFirstName_BookingExists_returnOk(){
        request
            .when()
                .queryParam("firstName", faker.name().firstName())
                .get("/booking")
            .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
            .and()
                .body("results", hasSize(greaterThan(0)))
        ;
    }

    @Test
    public void  getAllBookingsByPrice_BookingExists_returnOk(){
        request
            .when()
                .queryParam("totalprice", 1000)
                .get("/booking")
            .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
            .and()
                .body("results", hasSize(greaterThan(0)))
        ;
    }

    @Test // Delete booking
    public void DeleteBookingById_returnOk(){
        request
            //.header("Authorization","Basic")
            .header("Cookie", "token=3390c953ce6c173")
            .when()
                .delete("/booking/" + faker.number().digits(5))
            .then()
                .assertThat()
                .statusCode(201)
        ;
    }

    @Test // Health check
    public void ApiIsUpCheck_returnCreated(){
        request
            .when()
                .get("/ping")
            .then()
                .assertThat()
                .statusCode(201)
        ;
    }
}