package com.sportygroup.jackpot.service;

import com.sportygroup.jackpot.domain.ContributionType;
import com.sportygroup.jackpot.domain.Jackpot;
import com.sportygroup.jackpot.domain.RewardType;
import com.sportygroup.jackpot.dto.CreateJackpotCommand;
import com.sportygroup.jackpot.dto.JackpotDto;
import com.sportygroup.jackpot.exception.InvalidJackpotConfigException;
import com.sportygroup.jackpot.repository.JackpotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JackpotServiceTest {

    @Mock
    private JackpotRepository jackpotRepository;

    @InjectMocks
    private JackpotService jackpotService;

    private static CreateJackpotCommand fixedCommand(String id) {
        return new CreateJackpotCommand(id, new BigDecimal("1000.00"),
                new CreateJackpotCommand.ContributionSpec(ContributionType.FIXED, new BigDecimal("0.10"), null, null),
                new CreateJackpotCommand.RewardSpec(RewardType.FIXED, new BigDecimal("0.01"), null));
    }

    @Test
    void generatesIdWhenAbsent() {
        when(jackpotRepository.findById(any())).thenReturn(Optional.empty());
        when(jackpotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JackpotDto dto = jackpotService.create(fixedCommand(null));

        assertThat(dto.id()).isNotBlank();
        assertThat(dto.currentPool()).isEqualByComparingTo("1000.00");
    }

    @Test
    void duplicateIdIsIdempotentAndDoesNotSave() {
        Jackpot existing = new Jackpot("jackpot-1", new BigDecimal("999.00"),
                ContributionType.FIXED, null, RewardType.FIXED, null);
        when(jackpotRepository.findById("jackpot-1")).thenReturn(Optional.of(existing));

        JackpotDto dto = jackpotService.create(fixedCommand("jackpot-1"));

        assertThat(dto.id()).isEqualTo("jackpot-1");
        assertThat(dto.currentPool()).isEqualByComparingTo("999.00");
        verify(jackpotRepository, never()).save(any());
    }

    @Test
    void rejectsInitialPctOutOfRange() {
        CreateJackpotCommand bad = new CreateJackpotCommand("j", new BigDecimal("100"),
                new CreateJackpotCommand.ContributionSpec(ContributionType.FIXED, new BigDecimal("1.5"), null, null),
                new CreateJackpotCommand.RewardSpec(RewardType.FIXED, new BigDecimal("0.01"), null));
        when(jackpotRepository.findById("j")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jackpotService.create(bad))
                .isInstanceOf(InvalidJackpotConfigException.class);
    }

    @Test
    void rejectsVariableContributionMissingThreshold() {
        CreateJackpotCommand bad = new CreateJackpotCommand("j", new BigDecimal("100"),
                new CreateJackpotCommand.ContributionSpec(ContributionType.VARIABLE, new BigDecimal("0.20"), new BigDecimal("0.05"), null),
                new CreateJackpotCommand.RewardSpec(RewardType.FIXED, new BigDecimal("0.01"), null));
        when(jackpotRepository.findById("j")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jackpotService.create(bad))
                .isInstanceOf(InvalidJackpotConfigException.class);
    }
}
