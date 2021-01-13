
/*
 * @author : Dilanka Dilshan<ehd.dilanka@gmail.com>
 */

package lk.dila.web;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import lk.dila.mode.Customer;
import lk.dila.mode.Item;
import org.apache.commons.dbcp2.BasicDataSource;

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

@WebServlet(name = "ItemServlet", urlPatterns = "/item")
public class ItemServlet extends HttpServlet {

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        resp.addHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.addHeader("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Access-Control-Allow-Origin", "http://localhost:3000");

        String code = req.getParameter("code");
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        resp.setContentType("application/json");

        try (Connection connection = cp.getConnection()) {
            PrintWriter out = resp.getWriter();
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM Items" + ((code != null) ? " WHERE code=?" : ""));
            if (code != null) {
                pstm.setObject(1, code);
            }
            ResultSet resultSet = pstm.executeQuery();
            List<Item> itemList = new ArrayList<>();
            while (resultSet.next()) {
                code = resultSet.getString(1);
                String description = resultSet.getString(2);
                int qty = resultSet.getInt(3);
                String unitPrice = resultSet.getBigDecimal(4).setScale(2).toPlainString();
                itemList.add(new Item(code,description,qty,unitPrice));
            }

            if (code != null && itemList.isEmpty()) {
                //resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                Jsonb jsonb = JsonbBuilder.create();
                out.println(jsonb.toJson(itemList));
                connection.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Access-Control-Allow-Origin", "http://localhost:3000");

        resp.setContentType("application/json");
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");

        try (Connection connection = cp.getConnection();) {
            Jsonb jsonb = JsonbBuilder.create();
            Item item = jsonb.fromJson(req.getReader(), Item.class);

            if (item.getCode() == null || item.getDescription() == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (item.getCode().matches("c\\d{3}") || item.getDescription().trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            PreparedStatement pstm = connection.prepareStatement("INSERT INTO Items VALUES (?,?,?,?)");
            pstm.setString(1, item.getCode());
            pstm.setString(2, item.getDescription());
            pstm.setInt(3, item.getQty());
            pstm.setDouble(4, Double.parseDouble(item.getUnitPrice()));
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
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.addHeader("Access-Control-Allow-Origin", "http://localhost:3000");

        String code = req.getParameter("code");
        if (code == null && !code.matches("C\\d{3}")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        try (Connection connection = cp.getConnection()) {
            Jsonb jsonb = JsonbBuilder.create();
            Item item = jsonb.fromJson(req.getReader(), Item.class);

            if (item.getCode() == null || item.getDescription() == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (item.getCode().matches("c\\d{3}") || item.getDescription().trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM Items WHERE code=?");
            pstm.setObject(1, code);
            if (pstm.executeQuery().next()) {
                pstm = connection.prepareStatement("UPDATE Items SET description=?, qty=?, unitPrice=? where code=?");
                pstm.setObject(1, item.getDescription());
                pstm.setObject(2, item.getQty());
                pstm.setObject(3, item.getUnitPrice());
                pstm.setObject(4, code);
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

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Access-Control-Allow-Origin", "http://localhost:3000");

        String code = req.getParameter("code");
        if (code == null && !code.matches("C\\d{3}")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");

        try (Connection connection = cp.getConnection()) {
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM Items WHERE code=?");
            pstm.setObject(1, code);
            if (pstm.executeQuery().next()) {
                pstm = connection.prepareStatement("DELETE FROM Items where code=?");
                pstm.setObject(1, code);
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
}
