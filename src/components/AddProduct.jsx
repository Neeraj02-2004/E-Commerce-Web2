import React, { useEffect, useState } from "react";
import API from "../axios";
import { useNavigate } from "react-router-dom";

const AddProduct = () => {
  const navigate = useNavigate();

  const [product, setProduct] = useState({
    name: "",
    brand: "",
    description: "",
    price: "",
    category: "",
    stockQuantity: "",
    releaseDate: "",
    productAvailable: false,
  });

  const [image, setImage] = useState(null);
  const [popup, setPopup] = useState({
    show: false,
    message: "",
    type: "success",
  });

  const isAdmin = localStorage.getItem("role") === "ADMIN";
  const token = localStorage.getItem("token");

  const showPopup = (message, type = "success") => {
    setPopup({
      show: true,
      message,
      type,
    });

    setTimeout(() => {
      setPopup({
        show: false,
        message: "",
        type: "success",
      });
    }, 2500);
  };

  useEffect(() => {
    if (!token || !isAdmin) {
      showPopup("Only admin can add products", "error");

      setTimeout(() => {
        navigate("/");
      }, 1200);
    }
  }, [token, isAdmin, navigate]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;

    setProduct({
      ...product,
      [name]: value,
    });
  };

  const handleImageChange = (e) => {
    setImage(e.target.files[0]);
  };

  const submitHandler = async (event) => {
    event.preventDefault();

    if (!token || !isAdmin) {
      showPopup("Only admin can add products", "error");
      return;
    }

    if (!image) {
      showPopup("Please select product image", "error");
      return;
    }

    const productData = {
      ...product,
      price: Number(product.price),
      stockQuantity: Number(product.stockQuantity),
    };

    const formData = new FormData();
    formData.append("product", JSON.stringify(productData));
    formData.append("imageFile", image);

    try {
      const response = await API.post("/admin/product", formData);

      console.log("Product added successfully:", response.data);
      showPopup("Product added successfully", "success");

      setTimeout(() => {
        navigate("/");
      }, 1200);
    } catch (error) {
      console.error("Error adding product:", error);
      showPopup(error.response?.data?.message || "Error adding product", "error");
    }
  };

  if (!token || !isAdmin) {
    return (
      <>
        {popup.show && (
          <div style={popupStyle(popup.type)}>
            <div style={popupIconStyle}>{popup.type === "success" ? "✓" : "!"}</div>
            <div>{popup.message}</div>
          </div>
        )}
      </>
    );
  }

  return (
    <div className="container">
      {popup.show && (
        <div style={popupStyle(popup.type)}>
          <div style={popupIconStyle}>{popup.type === "success" ? "✓" : "!"}</div>
          <div>{popup.message}</div>
        </div>
      )}

      <div className="center-container">
        <form className="row g-3 pt-5" onSubmit={submitHandler}>
          <div className="col-md-6">
            <label className="form-label">
              <h6>Name</h6>
            </label>
            <input
              type="text"
              className="form-control"
              placeholder="Product Name"
              onChange={handleInputChange}
              value={product.name}
              name="name"
              required
            />
          </div>

          <div className="col-md-6">
            <label className="form-label">
              <h6>Brand</h6>
            </label>
            <input
              type="text"
              name="brand"
              className="form-control"
              placeholder="Enter your Brand"
              value={product.brand}
              onChange={handleInputChange}
              id="brand"
              required
            />
          </div>

          <div className="col-12">
            <label className="form-label">
              <h6>Description</h6>
            </label>
            <input
              type="text"
              className="form-control"
              placeholder="Add product description"
              value={product.description}
              name="description"
              onChange={handleInputChange}
              id="description"
              required
            />
          </div>

          <div className="col-5">
            <label className="form-label">
              <h6>Price</h6>
            </label>
            <input
              type="number"
              className="form-control"
              placeholder="Eg: 1000"
              onChange={handleInputChange}
              value={product.price}
              name="price"
              id="price"
              required
            />
          </div>

          <div className="col-md-6">
            <label className="form-label">
              <h6>Category</h6>
            </label>
            <select
              className="form-select"
              value={product.category}
              onChange={handleInputChange}
              name="category"
              id="category"
              required
            >
              <option value="">Select category</option>
              <option value="Laptop">Laptop</option>
              <option value="Headphone">Headphone</option>
              <option value="Mobile">Mobile</option>
              <option value="Electronics">Electronics</option>
              <option value="Toys">Toys</option>
              <option value="Fashion">Fashion</option>
              <option value="Boys Footwear">Boys Footwear</option>
              <option value="Girls Footwear">Girls Footwear</option>
              <option value="Boys Wear">Boys Wear</option>
              <option value="Girls Wear">Girls Wear</option>
              <option value="Girls Top">Girls Top</option>
              <option value="Girls pants">Girls pants</option>
              <option value="Child Wear">Child Wear</option>
              <option value="Mens Suit">Mens Suit</option>
              <option value="Womens Suit">Womens Suit</option>
              <option value="Womens Saree">Womens Saree</option>
              <option value="Mens Shirt">Mens Shirt</option>
              <option value="Mens pants">Mens Jeans</option>
              <option value="Grocery">Grocery</option>
            </select>
          </div>

          <div className="col-md-4">
            <label className="form-label">
              <h6>Stock Quantity</h6>
            </label>
            <input
              type="number"
              className="form-control"
              placeholder="Stock Remaining"
              onChange={handleInputChange}
              value={product.stockQuantity}
              name="stockQuantity"
              id="stockQuantity"
              required
            />
          </div>

          <div className="col-md-4">
            <label className="form-label">
              <h6>Release Date</h6>
            </label>
            <input
              type="date"
              className="form-control"
              value={product.releaseDate}
              name="releaseDate"
              onChange={handleInputChange}
              id="releaseDate"
              required
            />
          </div>

          <div className="col-md-4">
            <label className="form-label">
              <h6>Image</h6>
            </label>
            <input
              className="form-control"
              type="file"
              onChange={handleImageChange}
              accept="image/*"
              required
            />
          </div>

          <div className="col-12">
            <div className="form-check">
              <input
                className="form-check-input"
                type="checkbox"
                name="productAvailable"
                id="gridCheck"
                checked={product.productAvailable}
                onChange={(e) =>
                  setProduct({
                    ...product,
                    productAvailable: e.target.checked,
                  })
                }
              />
              <label className="form-check-label">Product Available</label>
            </div>
          </div>

          <div className="col-12">
            <button type="submit" className="btn btn-primary">
              Submit
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

const popupStyle = (type) => ({
  position: "fixed",
  top: "80px",
  right: "24px",
  zIndex: 9999,
  minWidth: "280px",
  maxWidth: "380px",
  padding: "14px 18px",
  borderRadius: "14px",
  color: "#fff",
  fontWeight: "600",
  display: "flex",
  alignItems: "center",
  gap: "12px",
  boxShadow: "0 12px 30px rgba(0,0,0,0.25)",
  background:
    type === "success"
      ? "linear-gradient(135deg, #16a34a, #22c55e)"
      : "linear-gradient(135deg, #dc2626, #f97316)",
});

const popupIconStyle = {
  width: "28px",
  height: "28px",
  borderRadius: "50%",
  background: "rgba(255,255,255,0.25)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  fontWeight: "bold",
  flexShrink: 0,
};

export default AddProduct;
