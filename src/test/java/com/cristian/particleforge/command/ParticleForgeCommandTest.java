package com.cristian.particleforge.command;

import com.cristian.particleforge.ParticleForgePlugin;
import com.cristian.particleforge.api.ParticleForgeApi;
import com.cristian.particleforge.cfg.MessageManager;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises the dispatch logic of {@link ParticleForgeCommand} without booting
 * Bukkit. Subcommands are mocked so we can assert routing decisions in
 * isolation (the real subs are smoke-tested at Task 24).
 */
class ParticleForgeCommandTest {

    private ParticleForgePlugin plugin;
    private ParticleForgeApi api;
    private MessageManager messages;
    private CommandSender sender;
    private Command bukkitCmd;
    private Subcommand fooSub;

    @BeforeEach
    void setUp() {
        plugin = mock(ParticleForgePlugin.class);
        api = mock(ParticleForgeApi.class);
        messages = mock(MessageManager.class);
        sender = mock(CommandSender.class);
        bukkitCmd = mock(Command.class);
        fooSub = mock(Subcommand.class);
        when(fooSub.name()).thenReturn("foo");
        when(fooSub.permission()).thenReturn("particleforge.foo");
        when(fooSub.descriptionKey()).thenReturn("help-desc-foo");
    }

    private ParticleForgeCommand build(Subcommand... subs) {
        return new ParticleForgeCommand(plugin, api, messages, List.of(subs));
    }

    @Test
    void emptyArgsSendsHelpHeader() {
        when(sender.hasPermission(anyString())).thenReturn(true);
        ParticleForgeCommand cmd = build(fooSub);
        when(messages.component(eq("help-desc-foo"))).thenReturn(
            net.kyori.adventure.text.Component.text("desc"));

        boolean handled = cmd.onCommand(sender, bukkitCmd, "pf", new String[0]);

        assertTrue(handled);
        verify(messages).send(eq(sender), eq("help-header"));
        verify(messages).send(eq(sender), eq("help-line"), any(TagResolver.class), any(TagResolver.class));
    }

    @Test
    void unknownSubSendsUnknownSubcommand() {
        ParticleForgeCommand cmd = build(fooSub);
        cmd.onCommand(sender, bukkitCmd, "pf", new String[]{"bogus"});
        verify(messages).send(eq(sender), eq("unknown-subcommand"), any(TagResolver.class));
        verify(fooSub, never()).execute(any(), any());
    }

    @Test
    void missingPermissionSendsNoPermission() {
        when(sender.hasPermission("particleforge.foo")).thenReturn(false);
        ParticleForgeCommand cmd = build(fooSub);
        cmd.onCommand(sender, bukkitCmd, "pf", new String[]{"foo", "arg1"});
        verify(messages).send(eq(sender), eq("no-permission"));
        verify(fooSub, never()).execute(any(), any());
    }

    @Test
    void permittedSubIsDispatchedWithRemainingArgs() {
        when(sender.hasPermission("particleforge.foo")).thenReturn(true);
        ParticleForgeCommand cmd = build(fooSub);
        cmd.onCommand(sender, bukkitCmd, "pf", new String[]{"foo", "a", "b"});
        ArgumentCaptor<String[]> args = ArgumentCaptor.forClass(String[].class);
        verify(fooSub).execute(eq(sender), args.capture());
        String[] captured = args.getValue();
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[]{"a", "b"}, captured);
    }

    @Test
    void helpLoopSkipsSubsWithoutPermission() {
        Subcommand admin = mock(Subcommand.class);
        when(admin.name()).thenReturn("admin");
        when(admin.permission()).thenReturn("particleforge.admin");
        when(admin.descriptionKey()).thenReturn("help-desc-admin");
        Subcommand pub = mock(Subcommand.class);
        when(pub.name()).thenReturn("pub");
        when(pub.permission()).thenReturn(null);
        when(pub.descriptionKey()).thenReturn("help-desc-pub");

        when(sender.hasPermission("particleforge.admin")).thenReturn(false);
        when(messages.component(anyString())).thenReturn(
            net.kyori.adventure.text.Component.text("desc"));

        ParticleForgeCommand cmd = build(admin, pub);
        cmd.onCommand(sender, bukkitCmd, "pf", new String[0]);

        // Only one help-line — for the null-permission "pub" sub.
        verify(messages, atLeastOnce()).send(eq(sender), eq("help-header"));
        verify(messages).send(eq(sender), eq("help-line"),
            any(TagResolver.class), any(TagResolver.class));
    }
}
