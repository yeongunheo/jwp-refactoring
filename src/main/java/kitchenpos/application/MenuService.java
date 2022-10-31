package kitchenpos.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuProduct;
import kitchenpos.domain.MenuProducts;
import kitchenpos.domain.Product;
import kitchenpos.exception.NotFoundException;
import kitchenpos.repository.MenuGroupRepository;
import kitchenpos.repository.MenuRepository;
import kitchenpos.repository.ProductRepository;
import kitchenpos.ui.dto.request.MenuCreateRequest;
import kitchenpos.ui.dto.request.MenuProductCreateRequest;
import kitchenpos.ui.dto.response.MenuCreateResponse;
import kitchenpos.ui.dto.response.MenuFindAllResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {

    private static final String NOT_FOUND_PRODUCT_ERROR_MESSAGE = "존재하지 않는 상품입니다.";
    private static final String NOT_FOUND_MENU_GROUP_ERROR_MESSAGE = "존재하지 않는 메뉴그룹입니다.";

    private final MenuRepository menuRepository;
    private final MenuGroupRepository menuGroupRepository;
    private final ProductRepository productRepository;

    public MenuService(
            final MenuRepository menuRepository,
            final MenuGroupRepository menuGroupRepository,
            final ProductRepository productRepository
    ) {
        this.menuRepository = menuRepository;
        this.menuGroupRepository = menuGroupRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public MenuCreateResponse create(final MenuCreateRequest request) {
        validateMenuGroup(request.getMenuGroupId());
        
        final MenuProducts menuProducts = toMenuProducts(request);
        validatePrice(request, menuProducts);

        final Menu menu = menuRepository.save(toMenu(request, menuProducts));
        return MenuCreateResponse.from(menu);
    }

    private void validatePrice(final MenuCreateRequest request, final MenuProducts menuProducts) {
        final List<Product> products = findProducts(request.getMenuProducts());
        BigDecimal price = request.getPrice();
        final BigDecimal sum = menuProducts.calculateTotalAmount(products);
        if (price.compareTo(sum) > 0) {
            throw new IllegalArgumentException();
        }
    }

    private List<Product> findProducts(final List<MenuProductCreateRequest> menuProducts1) {
        return menuProducts1.stream()
                .map(MenuProductCreateRequest::getProductId)
                .map(productId -> productRepository.findById(productId)
                        .orElseThrow(() -> new NotFoundException(NOT_FOUND_PRODUCT_ERROR_MESSAGE)))
                .collect(Collectors.toList());
    }

    private Menu toMenu(final MenuCreateRequest request, final MenuProducts menuProducts) {
        return Menu.builder()
                .name(request.getName())
                .price(request.getPrice())
                .menuGroupId(request.getMenuGroupId())
                .menuProducts(menuProducts)
                .build();
    }

    private MenuProducts toMenuProducts(final MenuCreateRequest request) {
        final List<MenuProduct> menuProducts = new ArrayList<>();
        for (final MenuProductCreateRequest menuProductsRequest : request.getMenuProducts()) {
            final Long productId = menuProductsRequest.getProductId();
            final long quantity = menuProductsRequest.getQuantity();
            final Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new NotFoundException(NOT_FOUND_PRODUCT_ERROR_MESSAGE));
            menuProducts.add(new MenuProduct(product.getId(), quantity));
        }
        return new MenuProducts(menuProducts);
    }

    private void validateMenuGroup(final Long menuGroupId) {
        if (!menuGroupRepository.existsById(menuGroupId)) {
            throw new NotFoundException(NOT_FOUND_MENU_GROUP_ERROR_MESSAGE);
        }
    }

    public List<MenuFindAllResponse> list() {
        final List<Menu> menus = menuRepository.findAll();
        List<MenuFindAllResponse> responses = MenuFindAllResponse.from(menus);
        return responses;
    }
}
