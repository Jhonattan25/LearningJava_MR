package com.learning.webflux.app.controller;

import com.learning.webflux.app.models.documents.Category;
import com.learning.webflux.app.models.documents.Product;
import com.learning.webflux.app.models.services.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@SessionAttributes("product")
@Controller
public class ProductController {
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @Value("${config.uploads.path}")
    private String imagesPath;

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @ModelAttribute("categories")
    public Flux<Category> categories() {
        return productService.findAllCategory();
    }

    @GetMapping("/uploads/img/{nameImage:.+}")
    public Mono<ResponseEntity<Resource>> showImage(@PathVariable String nameImage) throws MalformedURLException {
        Path path = Paths.get(imagesPath).resolve(nameImage).toAbsolutePath();

        Resource image = new UrlResource(path.toUri());

        return Mono.just(ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                        image.getFilename() + "\"")
                .body(image));
    }

    @GetMapping("/get/{id}")
    public Mono<String> get(Model model, @PathVariable String id) {
        return productService.findById(id)
                .doOnNext(p -> {
                    model.addAttribute("product", p);
                    model.addAttribute("title", "Detalle Producto");
                })
                .switchIfEmpty(Mono.just(new Product()))
                .flatMap(p -> {
                    if (p.getId() == null) {
                        return Mono.error(() -> new InterruptedException("No existe el producto"));
                    }
                    return Mono.just(p);
                })
                .then(Mono.just("show"))
                .onErrorResume(ex -> Mono.just("redirect:/list?error=no+existe+el+producto"));
    }

    @GetMapping({"/list", "/"})
    public Mono<String> list(Model model) {
        Flux<Product> products = productService.findAllWithNameUpperCase();
        products.subscribe(product -> log.info(product.getName()));

        model.addAttribute("title", "Listado de productos");
        model.addAttribute("products", products);

        return Mono.just("list");
    }

    @GetMapping("/form")
    public Mono<String> create(Model model, SessionStatus sessionStatus) {

        sessionStatus.setComplete();

        model.addAttribute("title", "Formulario de producto");
        model.addAttribute("button", "Crear");
        model.addAttribute("product", new Product());

        return Mono.just("form");
    }

    @GetMapping("/form/{id}")
    public Mono<String> edit(@PathVariable String id, Model model) {
        Mono<Product> productMono = productService.findById(id)
                .doOnNext(p -> log.info("Producto: " + p.getName()))
                .defaultIfEmpty(new Product());

        model.addAttribute("title", "Editar Producto");
        model.addAttribute("button", "Editar");
        model.addAttribute("product", productMono);

        return Mono.just("form");
    }

    @GetMapping("/form-v2/{id}")
    public Mono<String> editV2(@PathVariable String id, Model model) {
        return productService.findById(id)
                .doOnNext(p -> {
                    log.info("Producto: " + p.getName());
                    model.addAttribute("title", "Editar Producto");
                    model.addAttribute("button", "Editar");
                    model.addAttribute("product", p);
                })
                .defaultIfEmpty(new Product())
                .flatMap(p -> {
                    if (p.getId() == null) {
                        return Mono.error(() -> new InterruptedException("No existe el producto"));
                    }
                    return Mono.just(p);
                })
                .then(Mono.just("form"))
                .onErrorResume(ex -> Mono.just("redirect:/list?error=no+existe+el+producto"));
    }

    @PostMapping("/form")
    public Mono<String> save(@Valid Product newProduct, BindingResult result, Model model,
                             @RequestPart FilePart file) {

        System.out.println("LLEGA ANTES DE VALIDAR ERRORES");
        if (result.hasErrors()) {
            System.out.println("DETECTO UN ERROR: " + result.getAllErrors());
            model.addAttribute("title", "Errores en formulario producto");
            model.addAttribute("button", "Guardar");
            return Mono.just("form");
        }

        System.out.println("NO DETECTO ERRORES");

        return productService.findCategoryById(newProduct.getCategory().getId())
                .flatMap(c -> {
                    if (newProduct.getCreateAt() == null) {
                        newProduct.setCreateAt(LocalDateTime.now());
                    }

                    if (!file.filename().isEmpty()) {
                        newProduct.setImage(UUID.randomUUID().toString() + "-" + file.filename()
                                .replace(" ", "")
                                .replace(":", "")
                                .replace("\\", ""));
                    }

                    newProduct.setCategory(c);
                    return productService.save(newProduct)
                            .doOnNext(product -> {
                                log.info("Assigned category: {} Id: {}", product.getCategory().getName(),
                                        product.getCategory().getId());
                                log.info("Saved product: {} Id: {}", product.getName(), product.getId());
                            })
                            .flatMap(p -> {
                                if (!file.filename().isEmpty()) {
                                    return file.transferTo(new File(
                                            imagesPath + p.getImage()));
                                }
                                return Mono.empty();
                            })
                            .thenReturn("redirect:/list?success=producto+guardado+con+exito");
                });
    }

    @GetMapping("delete/{id}")
    public Mono<String> delete(@PathVariable String id) {
        return productService.findById(id)
                .defaultIfEmpty(new Product())
                .flatMap(p -> {
                    if (p.getId() == null) {
                        return Mono.error(new InterruptedException("No existe el producto a eliminar"));
                    }
                    log.info("Eliminando producto id: {}", p.getId());
                    log.info("Eliminando producto: {}", p.getName());
                    return productService.delete(p);
                })
                .then(Mono.just("redirect:/list?success=producto+eliminado+con+exito"))
                .onErrorResume(ex -> Mono.just("redirect:/list?error=no+existe+el+producto+a+eliminar"));
    }

    @GetMapping("/list-data-driver")
    public String listDataDriver(Model model) {
        Flux<Product> products = productService.findAllWithNameUpperCase()
                .delayElements(Duration.ofSeconds(1));
        products.subscribe(product -> log.info(product.getName()));

        model.addAttribute("title", "Listado de productos");
        model.addAttribute("products", new ReactiveDataDriverContextVariable(products, 1));

        return "list";
    }

    @GetMapping("/list-full")
    public String listFull(Model model) {
        Flux<Product> products = productService.findAllWithNameUpperCaseRepeat();

        model.addAttribute("title", "Listado de productos");
        model.addAttribute("products", products);

        return "list";
    }

    @GetMapping("/list-chunked")
    public String listChunked(Model model) {
        Flux<Product> products = productService.findAllWithNameUpperCaseRepeat();

        model.addAttribute("title", "Listado de productos");
        model.addAttribute("products", products);

        return "list-chunked";
    }
}
