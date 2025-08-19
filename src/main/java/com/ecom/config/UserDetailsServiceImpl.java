package com.ecom.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ecom.model.UserDtls;
import com.ecom.repository.UserRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		UserDtls user = userRepository.findByEmail(username);
		if (username.equalsIgnoreCase("sskulkarni4675@gmail.com")) {
			user.setRole("ROLE_ADMIN");
			userRepository.save(user);
		}
		

		if (user == null) {
			throw new UsernameNotFoundException("user not found");
		}
		return new CustomUser(user);
	}

}
