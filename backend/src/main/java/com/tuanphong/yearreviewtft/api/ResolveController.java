package com.tuanphong.yearreviewtft.api;

import com.tuanphong.yearreviewtft.api.dto.RiotAccountResponse;
import com.tuanphong.yearreviewtft.riot.TftMatchService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin // for frontend on :3000
@Validated
public class ResolveController {

    private final TftMatchService tftMatchService;

    public ResolveController(TftMatchService tftMatchService) {
        this.tftMatchService = tftMatchService;
    }

    @GetMapping("/resolve")
    public RiotAccountResponse resolve(@RequestParam @NotBlank String riotId) {
        // expects: Name#TAG
        return tftMatchService.resolveRiotId(riotId);
    }
}
