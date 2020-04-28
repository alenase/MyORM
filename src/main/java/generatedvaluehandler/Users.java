package generatedvaluehandler;

import annotations.GeneratedValue;
import annotations.Table;
import enums.GenerationType;

@Table(name = "users")
public class Users {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    private int age;
}
