package lk.kelaniya.uok.scrabble.scrabbleapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CORSConfig {
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource(){
//        var corsConfiguration=new CorsConfiguration();
//        corsConfiguration.setAllowedOrigins(List.of("http://localhost:3000"));
//        corsConfiguration.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
//        corsConfiguration.setAllowedHeaders(List.of("*")); //check this one
//        corsConfiguration.setExposedHeaders(List.of( "Content-Type"));
//        corsConfiguration.setAllowCredentials(true);
//
//        var source=new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**",corsConfiguration);
//        return source;
//    }


    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")   //.allowedOrigins("http://localhost:3000")
                        .allowedMethods("GET", "POST", "PUT","PATCH", "DELETE","OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }

}
