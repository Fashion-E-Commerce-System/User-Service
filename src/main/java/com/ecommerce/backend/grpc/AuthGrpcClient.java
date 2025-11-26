package com.ecommerce.backend.grpc;

import com.ecommerce.backend.domain.User;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthGrpcClient {

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;

    public void callCreateUser(User user) {
        log.info("gRPC callCreateUser for user: {}", user.getName());
        try {
            CreateUserRequest request = CreateUserRequest.newBuilder()
                    .setName(user.getName())
                    .setPassword(user.getPassword())
                    .build();

            AuthServiceResponse response = authServiceBlockingStub.createUser(request);

            if (!response.getSuccess()) {
                log.error("gRPC createUser failed: {}", response.getMessage());
                throw new RuntimeException("gRPC call to Auth-Service failed: " + response.getMessage());
            }
            log.info("gRPC createUser successful for user: {}", user.getName());
        } catch (StatusRuntimeException e) {
            log.error("gRPC call to Auth-Service failed with status: {}", e.getStatus());
            throw new RuntimeException("gRPC call to Auth-Service failed", e);
        }
    }

    public void callDeleteUser(Long userId) {
        log.info("gRPC callDeleteUser for userId: {}", userId);
        try {
            DeleteUserRequest request = DeleteUserRequest.newBuilder()
                    .setUserId(userId.toString())
                    .build();

            AuthServiceResponse response = authServiceBlockingStub.deleteUser(request);
            if (!response.getSuccess()) {
                log.error("gRPC deleteUser failed: {}", response.getMessage());
                throw new RuntimeException("gRPC call to Auth-Service failed: " + response.getMessage());
            }
            log.info("gRPC deleteUser successful for userId: {}", userId);
        } catch (StatusRuntimeException e) {
            log.error("gRPC call to Auth-Service failed with status: {}", e.getStatus());
            throw new RuntimeException("gRPC call to Auth-Service failed", e);
        }
    }
}
