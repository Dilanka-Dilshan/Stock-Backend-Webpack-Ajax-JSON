
/*
 * @author : Dilanka Dilshan<ehd.dilanka@gmail.com>
 */

package lk.dila.web;

import jakarta.json.Json;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.json.stream.JsonParsingException;
import lk.dila.mode.Customer;
import org.apache.commons.dbcp2.BasicDataSource;
import org.eclipse.yasson.internal.JsonBinding;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "CustomerServlet", urlPatterns = "/customer")
public class CustomerServlet extends HttpServlet {

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        resp.addHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.addHeader("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.addHeader("Access-Control-Allow-Origin", "http://localhost:3000");

        resp.setContentType("application/json");
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");

        try (Connection connection = cp.getConnection();) {
            Jsonb jsonb = JsonbBuilder.create();
            Customer customer = jsonb.fromJson(req.getReader(), Customer.class);

            if (customer.getId() == null || customer.getName() == null || customer.getAddress() == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (customer.getId().matches("c\\d{3}") || customer.getName().trim().isEmpty() || customer.getAddress().trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            PreparedStatement pstm = connection.prepareStatement("INSERT INTO Customers VALUES (?,?,?)");
            pstm.setString(1, customer.getId());
            pstm.setString(2, customer.getName());
            pstm.setString(3, customer.getAddress());
            if (pstm.executeUpdate() > 0) {
                resp.getWriter().println(jsonb.toJson(true));
            } else {
                resp.getWriter().println(jsonb.toJson(false));
            }
        } catch (SQLIntegrityConstraintViolationException ext) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (JsonbException exp) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.addHeader("Access-Control-Allow-Origin", "http://localhost:3000");

        String id = req.getParameter("id");
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        resp.setContentType("application/json");

        try (Connection connection = cp.getConnection()) {
            PrintWriter out = resp.getWriter();
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM Customers" + ((id != null) ? " WHERE id=?" : ""));
            if (id != null) {
                pstm.setObject(1, id);
            }
            ResultSet resultSet = pstm.executeQuery();
            List<Customer> customerList = new ArrayList<>();
            while (resultSet.next()) {
                id = resultSet.getString(1);
                String name = resultSet.getString(2);
                String address = resultSet.getString(3);
                customerList.add(new Customer(id, name, address));
            }

            if (id != null && customerList.isEmpty()) {
                //resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                Jsonb jsonb = JsonbBuilder.create();
                out.println(jsonb.toJson(customerList));
                connection.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Access-Control-Allow-Origin", "http://localhost:3000");

        String id = req.getParameter("id");
        if (id == null && !id.matches("C\\d{3}")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");

        try (Connection connection = cp.getConnection()) {
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM Customers WHERE id=?");
            pstm.setObject(1, id);
            if (pstm.executeQuery().next()) {
                pstm = connection.prepareStatement("DELETE FROM Customers where id=?");
                pstm.setObject(1, id);
                boolean success = pstm.executeUpdate() > 0;
                if (success) {
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (SQLIntegrityConstraintViolationException ext) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (SQLException throwables) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throwables.printStackTrace();
        }

    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.addHeader("Access-Control-Allow-Origin", "http://localhost:3000");

        String id = req.getParameter("id");
        if (id == null && !id.matches("C\\d{3}")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        try (Connection connection = cp.getConnection()) {
            Jsonb jsonb = JsonbBuilder.create();
            Customer customer = jsonb.fromJson(req.getReader(), Customer.class);

            if (customer.getId() != null || customer.getName() == null || customer.getAddress() == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (customer.getName().trim().isEmpty() || customer.getAddress().trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM Customers WHERE id=?");
            pstm.setObject(1, id);
            if (pstm.executeQuery().next()) {
                pstm = connection.prepareStatement("UPDATE Customers SET `name`=?, address=? where id=?");
                pstm.setObject(1, customer.getName());
                pstm.setObject(2, customer.getAddress());
                pstm.setObject(3, id);
                if (pstm.executeUpdate() > 0) {
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (JsonbException exp) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }

    }
}
