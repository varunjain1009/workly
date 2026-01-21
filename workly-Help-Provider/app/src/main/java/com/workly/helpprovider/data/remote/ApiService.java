package com.workly.helpprovider.data.remote;

import com.workly.helpprovider.data.model.Profile;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface ApiService {
    @POST("auth/otp")
    Call<ApiResponse<Void>> requestOtp(@Body OtpRequest request);

    @POST("auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("auth/refresh")
    Call<ApiResponse<AuthResponse>> refresh();

    @GET("profiles/worker")
    Call<ApiResponse<Profile>> getProfile();

    @POST("profiles/worker")
    Call<ApiResponse<Profile>> updateProfile(@Body Profile profile);

    @retrofit2.http.PATCH("profiles/worker/availability")
    Call<ApiResponse<Void>> updateAvailability(@retrofit2.http.Query("available") boolean available);

    @POST("location/update")
    Call<Void> updateLocation(@Body Map<String, Double> location);

    @GET("jobs/available")
    Call<ApiResponse<java.util.List<com.workly.helpprovider.data.model.Job>>> getAvailableJobs();

    @POST("jobs/{jobId}/accept")
    Call<ApiResponse<Void>> acceptJob(@retrofit2.http.Path("jobId") String jobId);

    @POST("jobs/{jobId}/complete")
    Call<ApiResponse<Void>> completeJob(@retrofit2.http.Path("jobId") String jobId, @Body Map<String, String> body);

    @POST("profile/device-token")
    Call<ApiResponse<Void>> updateDeviceToken(@Body Map<String, String> tokenMap);

    @GET("reviews/worker/{mobileNumber}/average")
    Call<ApiResponse<Double>> getAverageRating(@retrofit2.http.Path("mobileNumber") String mobileNumber);

    @GET("reviews/worker/{mobileNumber}")
    Call<ApiResponse<java.util.List<com.workly.helpprovider.data.model.ReviewDTO>>> getReviews(
            @retrofit2.http.Path("mobileNumber") String mobileNumber);

    @GET("config/public")
    Call<ApiResponse<com.workly.helpprovider.data.model.ConfigResponse>> getPublicConfig();
}
