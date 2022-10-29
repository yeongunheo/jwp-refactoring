package kitchenpos.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import kitchenpos.dao.MenuGroupDao;
import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuProduct;
import kitchenpos.domain.MenuProducts;
import kitchenpos.domain.Product;
import kitchenpos.exception.NotFoundException;
import kitchenpos.repository.MenuRepository;
import kitchenpos.repository.ProductRepository;
import kitchenpos.ui.dto.CreateMenuProductsRequest;
import kitchenpos.ui.dto.CreateMenuRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {

    private static final String NOT_FOUND_PRODUCT_ERROR_MESSAGE = "존재하지 않는 상품 ID 입니다.";
    private static final String NOT_FOUND_MENU_GROUP_ERROR_MESSAGE = "존재하지 않는 메뉴그룹 ID 입니다.";

    private final MenuRepository menuRepository;
    private final MenuGroupDao menuGroupDao;
    private final ProductRepository productRepository;

    public MenuService(
            final MenuRepository menuRepository,
            final MenuGroupDao menuGroupDao,
            final ProductRepository productRepository
    ) {
        this.menuRepository = menuRepository;
        this.menuGroupDao = menuGroupDao;
        this.productRepository = productRepository;
    }

    @Transactional
    public Menu create(final CreateMenuRequest request) {
        validateMenuGroup(request.getMenuGroupId());

        final MenuProducts menuProducts = toMenuProducts(request);
        validatePrice(request.getPrice(), menuProducts);

        final Menu menu = toMenu(request, menuProducts);
        return menuRepository.save(menu);
    }

    private Menu toMenu(final CreateMenuRequest request, final MenuProducts menuProducts) {
        return Menu.builder()
                .name(request.getName())
                .price(request.getPrice())
                .menuGroupId(request.getMenuGroupId())
                .menuProducts(menuProducts)
                .build();
    }

    private void validatePrice(final BigDecimal price, final MenuProducts menuProducts) {
        BigDecimal sum = menuProducts.calculateTotalAmount();
        if (price.compareTo(sum) > 0) {
            throw new IllegalArgumentException();
        }
    }

    private MenuProducts toMenuProducts(final CreateMenuRequest request) {
        List<MenuProduct> menuProducts = new ArrayList<>();
        for (final CreateMenuProductsRequest menuProductsRequest : request.getMenuProducts()) {
            final Long productId = menuProductsRequest.getProductId();
            final long quantity = menuProductsRequest.getQuantity();
            final Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new NotFoundException(NOT_FOUND_PRODUCT_ERROR_MESSAGE));
            menuProducts.add(new MenuProduct(product, quantity));
        }
        return new MenuProducts(menuProducts);
    }

    private void validateMenuGroup(final Long menuGroupId) {
        if (!menuGroupDao.existsById(menuGroupId)) {
            throw new NotFoundException(NOT_FOUND_MENU_GROUP_ERROR_MESSAGE);
        }
    }

    public List<Menu> list() {
        return menuRepository.findAll();
    }
}
