package lk.kelaniya.uok.scrabble.scrabbleapp;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableJpaRepositories
@EnableWebMvc
@EntityScan(basePackages = "lk.kelaniya.uok.scrabble.scrabbleapp.entity")
public class ScrabbleappApplication {
	@Bean
	public ModelMapper modelMapper(){
		return  new ModelMapper();
	}

	public static void main(String[] args) {

		SpringApplication.run(ScrabbleappApplication.class, args);
	}

}
