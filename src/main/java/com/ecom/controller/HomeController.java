package com.ecom.controller;


import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;

import java.util.UUID;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Carousel;
import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.UserDtls;
import com.ecom.repository.CarouselRepository;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.FileUploadService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;

import io.micrometer.common.util.StringUtils;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

	@Autowired
	private FileUploadService fileUploadService;
	@Autowired
	private CategoryService categoryService;


	@Autowired
    private CarouselRepository carouselRepository;
	@Autowired
	private ProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private CartService cartService;

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {
		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			m.addAttribute("countCart", countCart);
		}

		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
	}

	@GetMapping("/")
	public String index(Model m) {
	    // Load active categories (limit 6)
	    List<Category> allActiveCategory = categoryService.getAllActiveCategory().stream()
	            .sorted((c1, c2) -> c2.getId().compareTo(c1.getId()))
	            .limit(6)
	            .toList();

	    // Load active products (limit 8)
	    List<Product> allActiveProducts = productService.getAllActiveProducts("").stream()
	            .sorted((p1, p2) -> p2.getId().compareTo(p1.getId()))
	            .limit(8)
	            .toList();

	    // Load dynamic carousel images (ordered by position 1, 2, 3)
	    List<Carousel> carouselImages = carouselRepository.findAll().stream()
	            .sorted(Comparator.comparingInt(Carousel::getPosition))
	            .limit(3)
	            .toList();

	    // Add data to model
	    m.addAttribute("category", allActiveCategory);
	    m.addAttribute("products", allActiveProducts);
	    m.addAttribute("carouselImages", carouselImages);

	    return "index";
	}


	@GetMapping("/signin")
	public String login() {
		return "login";
	}

	@GetMapping("/register")
	public String register() {
		return "register";
	}

	@GetMapping("/products")
	public String products(Model m, @RequestParam(value = "category", defaultValue = "") String category,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "9") Integer pageSize,
			@RequestParam(defaultValue = "") String ch) {

		List<Category> categories = categoryService.getAllActiveCategory();
		m.addAttribute("paramValue", category);
		m.addAttribute("categories", categories);

//		List<Product> products = productService.getAllActiveProducts(category);
//		m.addAttribute("products", products);
		Page<Product> page = null;
		if (StringUtils.isEmpty(ch)) {
			page = productService.getAllActiveProductPagination(pageNo, pageSize, category);
		} else {
			page = productService.searchActiveProductPagination(pageNo, pageSize, category, ch);
		}

		List<Product> products = page.getContent();
		m.addAttribute("products", products);
		m.addAttribute("productsSize", products.size());

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "product";
	}

	@GetMapping("/product/{id}")
	public String product(@PathVariable int id, Model m) {
		Product productById = productService.getProductById(id);
		m.addAttribute("product", productById);
		return "view_product";
	}

//	@PostMapping("/saveUser")
//	public String saveUser(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file, HttpSession session)
//			throws IOException {
//
//		Boolean existsEmail = userService.existsEmail(user.getEmail());
//
//		if (existsEmail) {
//			session.setAttribute("errorMsg", "Email already exist");
//		} else {
//			String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
//			user.setProfileImage(imageName);
//			UserDtls saveUser = userService.saveUser(user);
//
//			if (!ObjectUtils.isEmpty(saveUser)) {
//				if (!file.isEmpty()) {
//					File saveFile = new ClassPathResource("static/img").getFile();
//
//					Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator
//							+ file.getOriginalFilename());
//
////					System.out.println(path);
//					Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
//				}
//				session.setAttribute("succMsg", "Register successfully");
//			} else {
//				session.setAttribute("errorMsg", "something wrong on server");
//			}
//		}
//
//		return "redirect:/register";
//	}
	@PostMapping("/saveUser")
	public String saveUser(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file, HttpSession session)
			throws IOException {

		Boolean existsEmail = userService.existsEmail(user.getEmail());

		if (existsEmail) {
			session.setAttribute("errorMsg", "Email already exist");
		} else {
			// ✅ Upload to Cloudinary or use default
			String imageUrl = file.isEmpty() ? "https://res.cloudinary.com/dfjyh7t7y/image/upload/v1754046158/products/s2cjrw6gvq5vkizkbaw2.jpg" : fileUploadService.uploadFile(file);
			System.out.println(imageUrl);
			user.setProfileImage(imageUrl);

			UserDtls saveUser = userService.saveUser(user);

			if (!ObjectUtils.isEmpty(saveUser)) {
				session.setAttribute("succMsg", "Register successfully");
			} else {
				session.setAttribute("errorMsg", "something wrong on server");
			}
		}

		return "redirect:/register";
	}


//	Forgot Password Code 

	@GetMapping("/forgot-password")
	public String showForgotPassword() {
		return "forgot_password.html";
	}

	@PostMapping("/forgot-password")
	public String processForgotPassword(@RequestParam String email, HttpSession session, HttpServletRequest request)
			throws UnsupportedEncodingException, MessagingException {

		UserDtls userByEmail = userService.getUserByEmail(email);

		if (ObjectUtils.isEmpty(userByEmail)) {
			session.setAttribute("errorMsg", "Invalid email");
		} else {

			String resetToken = UUID.randomUUID().toString();
			userService.updateUserResetToken(email, resetToken);

			// Generate URL :
			// http://localhost:8080/reset-password?token=sfgdbgfswegfbdgfewgvsrg

			String url = CommonUtil.generateUrl(request) + "/reset-password?token=" + resetToken;

			Boolean sendMail = commonUtil.sendMail(url, email);

			if (sendMail) {
				session.setAttribute("succMsg", "Please check your email..Password Reset link sent");
			} else {
				session.setAttribute("errorMsg", "Somethong wrong on server ! Email not send");
			}
		}

		return "redirect:/forgot-password";
	}

	@GetMapping("/reset-password")
	public String showResetPassword(@RequestParam String token, HttpSession session, Model m) {

		UserDtls userByToken = userService.getUserByToken(token);

		if (userByToken == null) {
			m.addAttribute("msg", "Your link is invalid or expired !!");
			return "message";
		}
		m.addAttribute("token", token);
		return "reset_password";
	}

	@PostMapping("/reset-password")
	public String resetPassword(@RequestParam String token, @RequestParam String password, HttpSession session,
			Model m) {

		UserDtls userByToken = userService.getUserByToken(token);
		if (userByToken == null) {
			m.addAttribute("errorMsg", "Your link is invalid or expired !!");
			return "message";
		} else {
			userByToken.setPassword(passwordEncoder.encode(password));
			userByToken.setResetToken(null);
			userService.updateUser(userByToken);
			// session.setAttribute("succMsg", "Password change successfully");
			m.addAttribute("msg", "Password change successfully");

			return "message";
		}

	}

	@GetMapping("/search")
	public String searchProduct(@RequestParam String ch, Model m) {
		List<Product> searchProducts = productService.searchProduct(ch);
		m.addAttribute("products", searchProducts);
		List<Category> categories = categoryService.getAllActiveCategory();
		m.addAttribute("categories", categories);
		return "product";

	}

}
