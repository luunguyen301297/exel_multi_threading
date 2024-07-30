package com.example.demo.users_mng;

import com.example.demo.users_mng.utils.SimplePage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    @GetMapping("")
    SimplePage<User> getUsers(
            @PageableDefault(size = 10, page = 0) Pageable pageable)
    {
        return userService.getAll(pageable);
    }

    @GetMapping("/{id}")
    User getUser(@PathVariable long id) {
        return userService.getById(id);
    }

}
