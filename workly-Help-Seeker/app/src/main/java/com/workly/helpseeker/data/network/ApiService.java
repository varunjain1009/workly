package com.workly.helpseeker.data.network;

import com.workly.helpseeker.data.model.Job;
import com.workly.helpseeker.data.model.Worker;
import com.workly.helpseeker.data.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("auth/otp")
    Call<ApiResponse<Void>> requestOtp(@Body OtpRequest request);

    @POST("auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("auth/refresh")
    Call<ApiResponse<AuthResponse>> refresh();

    @POST("jobs")
    @Headers("Content-Type: application/json")
    Call<ApiResponse<Job>> postJob(@Body Job job);

    @GET("jobs")
    Call<ApiResponse<List<Job>>> getJobs(@Query("type") String type);

    @GET("workers/search")
    Call<ApiResponse<List<Worker>>> searchWorkers(
            @Query("skill") String skill,
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("radius") int radius);

    @POST("jobs/{id}/assign")
    Call<ApiResponse<Job>> assignWorker(@Path("id") String jobId, @Query("workerId") String workerId);

    @POST("jobs/{id}/verify-otp")
    Call<Job> verifyJobCompletion(@Path("id") String jobId, @Query("otp") String otp);

    @POST("reviews")
    Call<ApiResponse<Void>> submitReview(@Body ReviewRequest request);

    @retrofit2.http.PUT("jobs/{id}")
    Call<ApiResponse<Job>> updateJob(@Path("id") String jobId, @Body Job job);

    @retrofit2.http.PATCH("jobs/{id}/status")
    Call<ApiResponse<Job>> updateJobStatus(@Path("id") String jobId, @Query("status") String status);

    @GET("config/public")
    Call<ApiResponse<com.workly.helpseeker.data.model.ConfigResponse>> getPublicConfig();

    @GET("profiles/seeker")
    Call<ApiResponse<User>> getSeekerProfile();

    @POST("profiles/device-token")
    Call<ApiResponse<Void>> updateDeviceToken(@Body java.util.Map<String, String> tokenMap);

    @GET("skills/autocomplete")
    Call<java.util.List<String>> getSkillSuggestions(@retrofit2.http.Query("query") String query);
}
