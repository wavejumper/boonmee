(require 'json)

(defcustom boonmee-idle-time 0.5
  "Time after which to query quickinfo at point"
  :group 'boonmee
  :type 'float)

(defcustom boonmee-command "boonmee"
  "The location of the boonmee binary"
  :group 'boonmee
  :type 'string)

(defcustom boonmee-log-file (make-temp-file "boonmee.log")
  "The file name boonmee will log to"
  :group 'boonmee
  :type 'string)

(defun boonmee-handle-error (resp)
  (print "boonmee: error"))

(defun boonmee-handle-definition (resp)
  (let* ((data (plist-get resp :data))
         (start (plist-get data :start))
         (line (plist-get start :line))
         (column (plist-get start :offset))
         (file (plist-get data :file)))
    (find-file file)
    (goto-line line)
    (move-to-column column)))

(defun boonmee-handle-quickinfo (resp)
  (let* ((data (plist-get resp :data))
         (display-string (plist-get data :displayString)))
    (message display-string)))

(defun boonmee-handle-completion (resp)
  (print resp))

(defun boonmee-handle-response (process output)
  (ignore-errors
    (let* ((json-object-type 'plist)
           (resp (json-read-from-string output))
           (command (plist-get resp :command))
           (success (eq (plist-get resp :success) t)))
      (cond
       ((string= "error" command)
        (boonmee-handle-error resp))

       ((and (string= "definition" command) success)
        (boonmee-handle-definition resp))

       ((and (string= "quickinfo" command) success)
        (boonmee-handle-quickinfo resp))

       ((and (string= "completionInfo" command) success)
        (boonmee-handle-completion resp))

       ((not success) nil)
       (t (message (concat "boonmee: cannot handle command " command) ))))))

(defun boonmee-init ()
  (if (get-process "boonmee")
      nil
    (let ((proc (start-process "boonmee" "boonmee-out" boonmee-command "-L" boonmee-log-file)))
      (print (concat "boonmee: logging to " boonmee-log-file))
      (set-process-filter proc 'boonmee-handle-response))))

(defun boonmee-project-root (file)
  (when-let ((dir (file-name-directory (directory-file-name file))))
    (if (file-exists-p (concat dir "package.json"))
        dir
      (when (not (string= dir (file-name-directory (directory-file-name dir))))
        (boonmee-project-root dir)))))

(defun boonmee-request-id ()
  (number-to-string (float-time)))

(defun boonmee-req-file (file project-root)
  (if (buffer-modified-p (current-buffer))
      (let ((out-file (concat project-root ".boonmee/cache/" (file-name-nondirectory file))))
        (make-directory (file-name-directory out-file) :parents)
        (write-region (point-min) (point-max) out-file)
        out-file)
      file))

(defun boonmee-goto-definition ()
  (interactive)
  (when-let ((file (buffer-file-name))
             (root (boonmee-project-root file)))
    (let* ((line (string-to-number (format-mode-line "%l")))
           (offset (string-to-number (format-mode-line "%c")))
           (req-id (boonmee-request-id))
           (req-file (boonmee-req-file file root))
           (args (list :file req-file :line line :offset offset :projectRoot root))
           (req (json-encode (list :command "definition" :type "request" :requestId req-id :arguments args))))
      (process-send-string "boonmee" (concat req "~\n")))))

(defun boonmee-quickinfo ()
  (interactive)
  (when-let* ((file (buffer-file-name))
              (root (boonmee-project-root file)))
    (let* ((line (string-to-number (format-mode-line "%l")))
           (offset (string-to-number (format-mode-line "%c")))
           (req-id (boonmee-request-id))
           (req-file (boonmee-req-file file root))
           (args (list :file req-file :line line :offset offset :projectRoot root))
           (req (json-encode (list :command "quickinfo" :type "request" :requestId req-id :arguments args))))
      (process-send-string "boonmee" (concat req "~\n")))))

(defun boonmee-completions ()
  (interactive)
  (when-let* ((file (buffer-file-name))
              (root (boonmee-project-root file)))
    (let* ((line (string-to-number (format-mode-line "%l")))
           (offset (string-to-number (format-mode-line "%c")))
           (req-id (boonmee-request-id))
           (req-file (boonmee-req-file file root))
           (args (list :file req-file :line line :offset offset :projectRoot root))
           (req (json-encode (list :command "completions" :type "request" :requestId req-id :arguments args))))
      (process-send-string "boonmee" (concat req "~\n")))))

(defvar boonmee-global-timer nil
  "Timer to trigger quickinfo.")

(defun boonmee-intellisense ()
  (when (bound-and-true-p boonmee-mode)
    (boonmee-quickinfo)))

(define-minor-mode boonmee-mode
  "Clojure intellisense"
  :group 'boonmee
  (when boonmee-mode
    (progn
      (boonmee-init)
      (unless boonmee-global-timer
        (setq boonmee-global-timer
              (run-with-idle-timer boonmee-idle-time
                                   :repeat 'boonmee-intellisense))))))

(provide 'boonmee-mode)
