package lk.kelaniya.uok.scrabble.scrabbleapp.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.kelaniya.uok.scrabble.scrabbleapp.security.UserDetailServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Configuration
@Component
@RequiredArgsConstructor
@Order(1)
public class AuthFilter extends OncePerRequestFilter {
    private final JWTutils jwtutils;
    private final UserDetailServiceImpl userDetailServiceImpl;
    private final StringHttpMessageConverter stringHttpMessageConverter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            var jwtToken=getJWTToken(request);
            if(jwtToken!=null && jwtutils.validateToken(jwtToken)) {
                var userName=jwtutils.getUsernameFromToken(jwtToken);
                var userDetails=userDetailServiceImpl.loadUserByUsername(userName);

                var authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        filterChain.doFilter(request, response);
    }
    private String getJWTToken(HttpServletRequest request ) {
        String authHeader=request.getHeader("Authorization");
        if(StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }else{
            return null;
        }
    }
}
