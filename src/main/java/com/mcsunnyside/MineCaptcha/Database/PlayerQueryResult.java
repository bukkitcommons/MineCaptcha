package com.mcsunnyside.MineCaptcha.Database;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.Nullable;
@Data
@AllArgsConstructor
public class PlayerQueryResult {
    boolean success;
    @Nullable String username;
    @Nullable String ipAddress;
}
