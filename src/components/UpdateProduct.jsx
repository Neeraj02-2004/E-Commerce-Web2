import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import axios, { API_ORIGIN } from "../axios";

const UpdateProduct = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [product, setProduct] = useState({});
  const [image, setImage] = useState(null);
  const [existingImageUrl, setExistingImageUrl] = useState("");
  const [popup, setPopup] = useState({
    show: false,
    message: "",
    type: "success",
  });

  const [updateProduct, setUpdateProduct] = useState({
    id: null,
    name: "",
    description: "",
    brand: "",
    price: "",
    category: "",
    releaseDate: "",
    productAvailable: false,
    stockQuantity: "",
  });

  const token = localStorage.getItem("token");
  const isAdmin = localStorage.getItem("role") === "ADMIN";

  const showPopup = (message, type = "success") => {
    setPopup({ show: true, message, type });

    setTimeout(() => {
      setPopup({ show: false, message: "", type: "success" });
    }, 2500);
  };

  useEffect(() => {
    if (!token || !isAdmin) {
      showPopup("Only admin can update products", "error");

      setTimeout(() => {
        navigate("/");
      }, 1200);

      return;
    }

    const fetchProduct = async () => {
      try {
        const response = await axios.get(`${API_ORIGIN}/api/product/${id}`);
        const data = response.data;

        setProduct(data);

        setUpdateProduct({
          id: data.id ?? null,
          name: data.name ?? "",
          description: data.description ?? "",
          brand: data.brand ?? "",
          price: data.price ?? "",
          category: data.category ?? "",
          releaseDate: data.releaseDate ?? "",
          productAvailable: data.productAvailable ?? false,
          stockQuantity: data.stockQuantity ?? "",
        });

        if (data.imageUrl) {
          if (data.imageUrl.startsWith("http")) {
            setExistingImageUrl(data.imageUrl);
          } else {
            setExistingImageUrl(`${API_ORIGIN}${data.imageUrl}`);
          }
        }
      } catch (error) {
        console.error("Error fetching product:", error);
        showPopup(error.response?.data?.message || "Failed to load product", "error");
      }
    };

    fetchProduct();
  }, [id, token, isAdmin, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!token || !isAdmin) {
      showPopup("Only admin can update products", "error");
      return;
    }

    const productData = {
      ...updateProduct,
      id: Number(id),
      price: Number(updateProduct.price),
      stockQuantity: Number(updateProduct.stockQuantity),
    };

    const formData = new FormData();
    formData.append("product", JSON.stringify(productData));

    if (image) {
      formData.append("imageFile", image);
    }

    try {
      await axios.put(`${API_ORIGIN}/api/admin/product/${id}`, formData, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      showPopup("Product updated successfully", "success");

      setTimeout(() => {
        navigate("/");
      }, 1200);
    } catch (error) {
      console.error("Error updating product:", error);
      showPopup(error.response?.data?.message || "Failed to update product", "error");
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;

    setUpdateProduct((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleImageChange = (e) => {
    setImage(e.target.files[0] || null);
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

  const previewImageUrl = image ? URL.createObjectURL(image) : existingImageUrl;

  return (
    <div className="update-product-container">
      {popup.show && (
        <div style={popupStyle(popup.type)}>
          <div style={popupIconStyle}>{popup.type === "success" ? "✓" : "!"}</div>
          <div>{popup.message}</div>
        </div>
      )}

      <div className="center-container" style={{ marginTop: "7rem" }}>
        <h1>Update Product</h1>

        <form className="row g-3 pt-1" onSubmit={handleSubmit}>
          <div className="col-md-6">
            <label className="form-label">
              <h6>Name</h6>
            </label>
            <input
              type="text"
              className="form-control"
              value={updateProduct.name || ""}
              onChange={handleChange}
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
              className="form-control"
              value={updateProduct.brand || ""}
              onChange={handleChange}
              name="brand"
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
              value={updateProduct.description || ""}
              onChange={handleChange}
              name="description"
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
              value={updateProduct.price || ""}
              onChange={handleChange}
              name="price"
              required
            />
          </div>

          <div className="col-md-6">
            <label className="form-label">
              <h6>Category</h6>
            </label>
            <select
              className="form-select"
              value={updateProduct.category || ""}
              onChange={handleChange}
              name="category"
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
              <option value="Mens pants">Mens pants</option>
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
              value={updateProduct.stockQuantity || ""}
              onChange={handleChange}
              name="stockQuantity"
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
              value={updateProduct.releaseDate || ""}
              onChange={handleChange}
              name="releaseDate"
            />
          </div>

          <div className="col-md-4">
            <label className="form-label">
              <h6>Image</h6>
            </label>

            {previewImageUrl && (
              <img
                src={previewImageUrl}
                alt={product.imageName || "Product"}
                style={{
                  width: "100%",
                  height: "180px",
                  objectFit: "cover",
                  padding: "5px",
                }}
              />
            )}

            <input
              className="form-control"
              type="file"
              onChange={handleImageChange}
              accept="image/*"
            />
          </div>

          <div className="col-12">
            <div className="form-check">
              <input
                className="form-check-input"
                type="checkbox"
                checked={updateProduct.productAvailable}
                onChange={(e) =>
                  setUpdateProduct({
                    ...updateProduct,
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

export default UpdateProduct;