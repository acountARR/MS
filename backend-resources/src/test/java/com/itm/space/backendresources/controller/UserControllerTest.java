package com.itm.space.backendresources.controller;

import com.itm.space.backendresources.BaseIntegrationTest;
import com.itm.space.backendresources.api.request.UserRequest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import javax.ws.rs.core.Response;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "user", password = "root", authorities = "ROLE_MODERATOR")
public class UserControllerTest extends BaseIntegrationTest {

    @MockBean
    private Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realmItm;

    @SpyBean
    private RestExceptionHandler restExceptionHandler;

    private UserRequest testUserRequest;
    private UserRequest testInvalidUserRequest;

    private RealmResource realmResourceMock;    //взаимодействие с базой данных Realm.
    private UsersResource usersResourceMock;    //управление пользователями в Realm
    private UserRepresentation userRepresentationMock; //представление информации о пользователе
    private UserResource userResourceMock; //взаимодействие с конкретным пользователем в Realm
    private RoleMappingResource roleMappingResourceMock; //взаимодействие с ролями user


    @BeforeEach
    void initNecessaryMocks() {
        testUserRequest = new UserRequest("username", "username@gmail.com", "root", "firstname", "lastname");
        testInvalidUserRequest = new UserRequest("", "username@gmail.com", "root", "firstname", "lastname");

        // Создание mock классов для тестов
        realmResourceMock = mock(RealmResource.class);
        usersResourceMock = mock(UsersResource.class);
        userRepresentationMock = mock(UserRepresentation.class);
        userResourceMock = mock(UserResource.class);
        roleMappingResourceMock = mock(RoleMappingResource.class);
    }

    @Test
    //hello
    public void helloMethodTest() throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/api/users/hello")).andReturn().getResponse();
        assertEquals(HttpStatus.OK.value(), response.getStatus()); //проверяет, что статус ответа равен 200
        assertEquals("user", response.getContentAsString()); //Проверяет, что содержимое ответа ровно "user"
    }

    @Test
    @SneakyThrows
    //create user
    public void userCreatedTest_returnSuccessesStatus() {

        when(keycloak.realm(realmItm)).thenReturn(realmResourceMock);
        when(realmResourceMock.users()).thenReturn(usersResourceMock);
        when(usersResourceMock.create(any(UserRepresentation.class))).thenReturn(Response.status(Response.Status.CREATED).build());
        when(userRepresentationMock.getId()).thenReturn(UUID.randomUUID().toString());

        MockHttpServletResponse response = mvc.perform(requestWithContent(post("/api/users"), testUserRequest))
                .andReturn().getResponse(); //создается фиктивный ответ сервера с заданными параметрами

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        //проверяется, что статус ответа равен 200 (HttpStatus.OK.value()) и содержимое ответа равно "user"

        //проверяет вызов методов
        verify(keycloak).realm(realmItm);
        verify(realmResourceMock).users();
        verify(usersResourceMock).create(any(UserRepresentation.class));

    }

    @Test
    @SneakyThrows
    //exception create user
    public void createUser_ShouldCatchHandleExceptionTest() {

        when(keycloak.realm(realmItm)).thenReturn(realmResourceMock);
        when(realmResourceMock.users()).thenReturn(usersResourceMock);
        when(usersResourceMock.create(any())).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());

        MockHttpServletResponse response = mvc.perform(requestWithContent(post("/api/users"), testUserRequest))
                .andReturn().getResponse();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());

        verify(restExceptionHandler, times(1)).handleException(any());
    }

    @Test
    @SneakyThrows
    // get user
    public void getUserByIdTest_ShouldReturnUserIDSuccess() {
        UUID userId = UUID.randomUUID();

        when(keycloak.realm(realmItm)).thenReturn(realmResourceMock);
        when(realmResourceMock.users()).thenReturn(usersResourceMock);
        when(userResourceMock.roles()).thenReturn(roleMappingResourceMock);
        when(roleMappingResourceMock.getAll()).thenReturn(mock(MappingsRepresentation.class));

        when(realmResourceMock.users().get(eq(String.valueOf(userId)))).thenReturn(userResourceMock);
        when(userResourceMock.toRepresentation()).thenReturn(userRepresentationMock);
        when(userRepresentationMock.getId()).thenReturn(String.valueOf(userId));

        MockHttpServletResponse response = mvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());

    }

    @Test
    @SneakyThrows
    // get user exception
    public void getUserByIdTest_ShouldCatchHandleException() {
        UUID userId = UUID.randomUUID();

        when(keycloak.realm(realmItm)).thenReturn(realmResourceMock);
        when(realmResourceMock.users()).thenReturn(usersResourceMock);
        when(userResourceMock.roles()).thenReturn(roleMappingResourceMock);
        when(roleMappingResourceMock.getAll()).thenReturn(mock(MappingsRepresentation.class));

        when(realmResourceMock.users().get(String.valueOf(userId))).thenReturn(userResourceMock);
        when(userResourceMock.groups()).thenThrow(new RuntimeException("Some error message"));
        when(userResourceMock.toRepresentation()).thenReturn(userRepresentationMock);

        MockHttpServletResponse response = mvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isInternalServerError())
                .andReturn().getResponse();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
        verify(restExceptionHandler, times(1)).handleException(any());
    }

    @Test
    @SneakyThrows
    // invalid Argument
    public void userCreatedTest_ShouldHandleInvalidArgument() {
        MockHttpServletResponse response = mvc.perform(requestWithContent(post("/api/users"),
                testInvalidUserRequest)).andReturn().getResponse();
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());

    }


}
