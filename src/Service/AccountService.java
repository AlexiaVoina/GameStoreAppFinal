package Service;

import Model.*;
import Repository.IRepository;
import Exception.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import Exception.BusinessLogicException;
import Repository.InMemoryRepository;

/**
 * Service class for managing user accounts, including authentication, role-based signup, and account deletion.
 */
public class AccountService {
    private final IRepository<User> userRepository; //ambele
    private final IRepository<Admin> adminRepository;
    private final  IRepository<Developer> developerRepository;
    private final IRepository<Customer> customerRepository;
    private final IRepository<ShoppingCart> shoppingCartRepository;
    private User loggedInUser;

    /**
     * Constructs the AccountService with repositories for different user types.
     *
     * @param userRepository The repository for storing and retrieving users.
     * @param adminRepository The repository for storing and retrieving administrators.
     * @param developerRepository The repository for storing and retrieving developers.
     * @param customerRepository The repository for storing and retrieving customers.
     * @param shoppingCartRepository The repository for storing and retrieving shopping carts.
     */
    public AccountService(IRepository<User> userRepository, IRepository<Admin> adminRepository, IRepository<Developer> developerRepository, IRepository<Customer> customerRepository, IRepository<ShoppingCart> shoppingCartRepository) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository != null ? adminRepository : new InMemoryRepository<>();
        this.developerRepository = developerRepository != null ? developerRepository : new InMemoryRepository<>();
        this.customerRepository = customerRepository;
        this.shoppingCartRepository = shoppingCartRepository;
    }

    /**
     * Registers a new user with the given details and assigns a role based on the email domain.
     *
     * @param username The username for the new account.
     * @param email The email for the new account.
     * @param password The password for the new account.
     * @return true if registration is successful, false otherwise.
     * @throws BusinessLogicException if the email is already in use.
     */
    public boolean signUp(String username, String email, String password) {
        if (isEmailUsed(email)) {
            throw new BusinessLogicException("Email is already in use.");
        }

        String role = determineRoleByEmail(email);
        int userId;

        User newUser;
        switch (role) {
            case "Admin":
                userId = (adminRepository != null ? adminRepository.getAll().size() : userRepository.getAll().size()) + 1;
                newUser = new Admin(userId, username, email, password, role);
                if (adminRepository != null) {
                    adminRepository.create((Admin) newUser);
                } else {
                    userRepository.create(newUser);
                }
                break;
            case "Developer":
                userId = (developerRepository != null ? developerRepository.getAll().size() : userRepository.getAll().size()) + 1;
                newUser = new Developer(userId, username, email, password, role, new ArrayList<>());
                if (developerRepository != null) {
                    developerRepository.create((Developer) newUser);
                } else {
                    userRepository.create(newUser);
                }
                break;

            case "Customer":
                userId = (customerRepository != null ? customerRepository.getAll().size() : userRepository.getAll().size()) + 1;
                Customer newCustomer = new Customer(userId, username, email, password, role, 0.0f, new ArrayList<>(), new ArrayList<>(), null);
                ShoppingCart shoppingCart = new ShoppingCart(userId, newCustomer);
                newCustomer.setShoppingCart(shoppingCart);

                if (customerRepository != null) {
                    customerRepository.create(newCustomer);
                    shoppingCartRepository.create(shoppingCart);
                } else {
                    userRepository.create(newCustomer);
                    shoppingCartRepository.create(shoppingCart);
                }
                break;
        }

        return true;
    }


    /**
     * Authenticates a user with their email and password.
     * @param email The email of the user.
     * @param password The password of the user.
     * @return true if login is successful, false otherwise.
     * @throws BusinessLogicException if the email or password is incorrect.
     */
    public boolean logIn(String email, String password) {
        List<User> users = new ArrayList<>(userRepository.getAll());

        if (adminRepository != null) {
            users.addAll(adminRepository.getAll());
        }
        if (developerRepository != null) {
            users.addAll(developerRepository.getAll());
        }

        if (customerRepository != null) {
            users.addAll(customerRepository.getAll());
        }

        for (User u : users) {
            if (u.getEmail().equals(email) && u.getPassword().equals(password)) {
                loggedInUser = u;
                System.out.println("Successful authentication for user: " + loggedInUser.getUsername());
                return true;
            }
        }

        throw new BusinessLogicException("Wrong email or password.");
    }

    /**
     * Logs out the currently logged-in user.
     * @return true if the user was logged out successfully.
     * @throws BusinessLogicException if no user is logged in.
     */
    public boolean logOut() {
        if (loggedInUser != null) {
            loggedInUser = null;
            return true;
        }
        throw new BusinessLogicException("No user is logged in to log out.");
    }

    /**
     * Deletes the currently logged-in user's account.
     * @return true if the account was deleted, false otherwise.
     * @throws BusinessLogicException if no user is logged in or if the repository is not initialized.
     */
    public boolean deleteAccount() {
        if (loggedInUser != null) {
            switch (loggedInUser.getRole()) {
                case "Admin":
                    if (adminRepository != null) {
                        adminRepository.delete(loggedInUser.getId());
                    } else {
                        throw new BusinessLogicException("Admin repository is not initialized.");
                    }
                    break;
                case "Developer":
                    if (developerRepository != null) {
                        developerRepository.delete(loggedInUser.getId());
                    } else {
                        throw new BusinessLogicException("Developer repository is not initialized.");
                    }
                    break;
                case "Customer":
                    if (customerRepository != null) {
                        Customer customerToDelete = (Customer) loggedInUser;
                        ShoppingCart shoppingCart = customerToDelete.getShoppingCart();
                        if (shoppingCart != null) {
                            shoppingCartRepository.delete(shoppingCart.getId());
                        }
                        if (customerToDelete.getReviews() != null) {
                            customerToDelete.getReviews().clear();
                        }
                        if (customerToDelete.getGamesLibrary() != null) {
                            customerToDelete.getGamesLibrary().clear();
                        }
                        customerRepository.delete(customerToDelete.getId());
                    } else {
                        throw new BusinessLogicException("User repository is not initialized.");
                    }
                    break;
                default:
                    if (userRepository != null) {
                        userRepository.delete(loggedInUser.getId());
                    } else {
                        throw new BusinessLogicException("User repository is not initialized.");
                    }
                    break;
            }
            loggedInUser = null;
            return true;
        }
        throw new BusinessLogicException("No user is logged in to delete.");
    }


    /**
     * Checks if a given email is already in use by an existing user.
     * @param email The email to check.
     * @return true if the email is in use, false otherwise.
     */
    private boolean isEmailUsed(String email) {

        if (userRepository != null) {
            for (User user : userRepository.getAll()) {
                if (user.getEmail().equals(email)) {
                    return true;
                }
            }
        } else {
            for (Admin admin : adminRepository.getAll()) {
                if (admin.getEmail().equals(email)) {
                    return true;
                }
            }
            for (Developer developer : developerRepository.getAll()) {
                if (developer.getEmail().equals(email)) {
                    return true;
                }
            }

             for (Customer customer : customerRepository.getAll()) {
                 if (customer.getEmail().equals(email)) {
                     return true;
                 }
             }
        }
        return false;
    }

    /**
     * Determines the role of a user based on their email domain.
     * @param email The email of the user.
     * @return A string representing the role, either "Admin", "Developer", or "Customer".
     * @throws ValidationException if the email domain is invalid or unsupported.
     */
    private String determineRoleByEmail(String email) {
        if (email.endsWith("@adm.com")) {
            return "Admin";
        } else if (email.endsWith("@dev.com")) {
            return "Developer";
        } else if (email.endsWith("@gmail.com")) {
            return "Customer";
        } else {
            throw new ValidationException("Unsupported email domain: " + email);
        }
    }

    /**
     * Retrieves the currently logged-in user.
     * @return The logged-in user, or null if no user is logged in.
     */
    public User getLoggedInUser() {
        return loggedInUser;
    }
}
