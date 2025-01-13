package org.borg.backend.social.block.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("block")
@RequiredArgsConstructor
@Tag(name = "Block")
public class PlayerBlockController {
}
